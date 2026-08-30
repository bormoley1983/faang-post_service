package faang.school.postservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AiModerationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiModerationService aiModerationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiModerationService, "API_URL", "http://test-api/toxicity");
    }

    @Test
    void isToxic_whenTextNull_returnsFalse() {
        // Act
        boolean result = aiModerationService.isToxic(null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isToxic_whenTextBlank_returnsFalse() {
        // Act
        boolean result = aiModerationService.isToxic("   ");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isToxic_whenScoreAboveThreshold_returnsTrue() {
        // Arrange
        String jsonResponse = """
                {"attributeScores":{"TOXICITY":{"summaryScore":{"value":0.95}}}}
                """;
        when(restTemplate.exchange(eq("http://test-api/toxicity"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(jsonResponse));

        // Act
        boolean result = aiModerationService.isToxic("bad text");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isToxic_whenScoreBelowThreshold_returnsFalse() {
        // Arrange
        String jsonResponse = """
                {"attributeScores":{"TOXICITY":{"summaryScore":{"value":0.3}}}}
                """;
        when(restTemplate.exchange(eq("http://test-api/toxicity"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(jsonResponse));

        // Act
        boolean result = aiModerationService.isToxic("good text");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isToxic_whenApiFails_returnsFalse() {
        // Arrange
        when(restTemplate.exchange(eq("http://test-api/toxicity"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act
        boolean result = aiModerationService.isToxic("some text");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isToxic_whenResponseMalformed_returnsFalse() {
        // Arrange
        when(restTemplate.exchange(eq("http://test-api/toxicity"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not a json"));

        // Act
        boolean result = aiModerationService.isToxic("some text");

        // Assert
        assertThat(result).isFalse();
    }
}
