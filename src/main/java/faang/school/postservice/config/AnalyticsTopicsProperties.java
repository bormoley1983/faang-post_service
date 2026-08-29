package faang.school.postservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.kafka.topics.analytics")
public record AnalyticsTopicsProperties(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]+") String like) {
}
