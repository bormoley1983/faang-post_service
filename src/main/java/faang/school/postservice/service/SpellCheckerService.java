package faang.school.postservice.service;

import faang.school.postservice.config.SpellerConfig;
import faang.school.postservice.model.SpellCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SpellCheckerService {

    private final RestTemplate restTemplate;
    private final String spellerUrl;
    private final SpellerConfig spellerConfig;

    public SpellCheckerService(
            @Qualifier("PostServiceRestTemplate") RestTemplate restTemplate,
            @Value("${app.speller.url}") String spellerUrl,
            SpellerConfig spellerConfig) {
        this.restTemplate = restTemplate;
        this.spellerUrl = spellerUrl;
        this.spellerConfig = spellerConfig;
    }

    private UriComponents buildBaseUri() {
        return UriComponentsBuilder.fromUriString(spellerUrl)
                .path("/checkTexts")
                .build();
    }

    public int calculateBatchSize() {
        int availableLength = spellerConfig.getMaxRequestUriLength() - spellerConfig.getBaseUrlLength();
        int textWithSeparatorLength = spellerConfig.getMaxContentLength() + spellerConfig.getSeparatorLength();
        return Math.max(1, availableLength / textWithSeparatorLength);
    }

    @Retryable(maxAttemptsExpression = "${app.speller.retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${app.speller.retry.backoffDelay}"))
    public List<String> sendBatchRequestToYandexSpeller(List<String> texts) {
        URI requestUri = buildRequestUri(texts);
        SpellCheckResponse[][] responses = sendRequestToSpellerApi(requestUri);

        return processResponses(texts, responses);
    }

    private URI buildRequestUri(List<String> texts) {
        return UriComponentsBuilder.fromUri(buildBaseUri().toUri())
                .queryParam("text", texts.toArray())
                .build()
                .toUri();
    }

    private SpellCheckResponse[][] sendRequestToSpellerApi(URI requestUri) {
        log.info("Sending request to Yandex Speller API: {}", requestUri);
        SpellCheckResponse[][] responses = restTemplate.postForObject(requestUri,
                null,
                SpellCheckResponse[][].class);

        if (responses == null) {
            throw new IllegalStateException("Received null response from Yandex Speller API");
        }

        log.info("Received response from Yandex Speller API");
        return responses;
    }

    private List<String> processResponses(List<String> texts, SpellCheckResponse[][] responses) {
        if (responses.length != texts.size()) {
            throw new IllegalStateException("Number of responses does not match number of texts");
        }

        List<String> correctedTexts = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String originalText = texts.get(i);
            SpellCheckResponse[] responseArray = responses[i];
            String correctedText = applyCorrectionsToText(originalText, responseArray);
            correctedTexts.add(correctedText);
        }

        return correctedTexts;
    }

    private String applyCorrectionsToText(String originalText, SpellCheckResponse[] responses) {
        if (responses == null || responses.length == 0) {
            log.info("No errors found in text: {}", originalText);
            return originalText;
        }

        StringBuilder textBuilder = new StringBuilder(originalText);
        log.info("Applying corrections to text: {}", originalText);

        for (SpellCheckResponse response : responses) {
            if (response.getS() != null && !response.getS().isEmpty()) {
                applyCorrectionToWord(textBuilder, response);
            }
        }

        String correctedText = textBuilder.toString();
        log.info("Corrected text: {}", correctedText);
        return correctedText;
    }

    private void applyCorrectionToWord(StringBuilder textBuilder, SpellCheckResponse response) {
        String correction = response.getS().get(0);
        int startPos = response.getPos();
        int endPos = startPos + response.getLen();

        if (isPositionValid(startPos, endPos, textBuilder.length())) {
            textBuilder.replace(startPos, endPos, correction);
            log.info("Replaced '{}' with '{}' at position {}", response.getWord(), correction, startPos);
        } else {
            log.warn("Invalid position or length for word '{}'. Skipping correction.", response.getWord());
        }
    }

    private boolean isPositionValid(int startPos, int endPos, int textLength) {
        return startPos >= 0 && endPos <= textLength;
    }
}