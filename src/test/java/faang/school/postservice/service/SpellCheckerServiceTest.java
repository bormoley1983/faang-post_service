package faang.school.postservice.service;

import faang.school.postservice.config.SpellerConfig;
import faang.school.postservice.model.SpellCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpellCheckerServiceTest {

    private static final String SPELLER_URL = "https://speller.yandex.net/services/spellservice.json/checkText";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SpellerConfig spellerConfig;

    @InjectMocks
    private SpellCheckerService spellCheckerService;

    @BeforeEach
    void setUp() {
        spellCheckerService = new SpellCheckerService(restTemplate, SPELLER_URL, spellerConfig);
    }

    @Test
    void testCalculateBatchSize() {
        int maxRequestUriLength = 10000;
        int baseUrlLength = 70;
        int maxContentLength = 4096;
        int separatorLength = 6;
        when(spellerConfig.getMaxRequestUriLength()).thenReturn(maxRequestUriLength);
        when(spellerConfig.getBaseUrlLength()).thenReturn(baseUrlLength);
        when(spellerConfig.getMaxContentLength()).thenReturn(maxContentLength);
        when(spellerConfig.getSeparatorLength()).thenReturn(separatorLength);

        int batchSize = spellCheckerService.calculateBatchSize();

        assertEquals(2, batchSize);
    }

    @Test
    void testSendBatchRequestToYandexSpeller() {
        List<String> texts = List.of("Приветт", "Мирр");
        SpellCheckResponse[][] responses = new SpellCheckResponse[][]{
                {new SpellCheckResponse("Приветт", List.of("Привет"), 0, 7)},
                {new SpellCheckResponse("Мирр", List.of("Мир"), 0, 4)}
        };

        when(restTemplate.postForObject(any(URI.class), isNull(), eq(SpellCheckResponse[][].class)))
                .thenReturn(responses);

        List<String> correctedTexts = spellCheckerService.sendBatchRequestToYandexSpeller(texts);

        assertEquals(List.of("Привет", "Мир"), correctedTexts);
        verify(restTemplate, times(1)).postForObject(any(URI.class), isNull(), eq(SpellCheckResponse[][].class));
    }

    @Test
    void testSendBatchRequestToYandexSpeller_EmptyResponse() {
        List<String> texts = List.of("Приветт", "Мирр");

        when(restTemplate.postForObject(any(URI.class), isNull(), eq(SpellCheckResponse[][].class)))
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> spellCheckerService.sendBatchRequestToYandexSpeller(texts));
    }

    @Test
    void testSendBatchRequestToYandexSpeller_ResponseSizeMismatch() {
        List<String> texts = List.of("Приветт", "Мирр");
        SpellCheckResponse[][] responses = new SpellCheckResponse[][]{
                {new SpellCheckResponse("Приветт", List.of("Привет"), 0, 7)}
        };

        when(restTemplate.postForObject(any(URI.class), isNull(), eq(SpellCheckResponse[][].class)))
                .thenReturn(responses);

        assertThrows(IllegalStateException.class, () -> spellCheckerService.sendBatchRequestToYandexSpeller(texts));
    }
}