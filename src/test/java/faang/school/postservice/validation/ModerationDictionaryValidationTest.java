package faang.school.postservice.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class ModerationDictionaryValidationTest {

    private ModerationDictionaryValidation validation;

    @BeforeEach
    void setUp() {
        String json = """
                ["badword1", "badword2", "offensive"]
                """;
        ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "bad-words.json";
            }
        };
        validation = new ModerationDictionaryValidation(resource);
    }

    @Test
    void containsBadWord_whenTextContainsBadWord_returnsTrue() {
        // Act
        boolean result = validation.containsBadWord("this has badword1 in it");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void containsBadWord_whenTextIsCaseInsensitive_returnsTrue() {
        // Act
        boolean result = validation.containsBadWord("THIS HAS BADWORD2 IN IT");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void containsBadWord_whenNoBadWordsPresent_returnsFalse() {
        // Act
        boolean result = validation.containsBadWord("this is a perfectly fine text");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void containsBadWord_whenTextIsNull_returnsFalse() {
        // Act
        boolean result = validation.containsBadWord(null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void containsBadWord_whenTextIsBlank_returnsFalse() {
        // Act
        boolean result = validation.containsBadWord("   ");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void constructor_whenResourceThrowsIOException_wrapsInRuntimeException() {
        // Arrange
        ByteArrayResource badResource = new ByteArrayResource(new byte[0]) {
            @Override
            public java.io.InputStream getInputStream() throws java.io.IOException {
                throw new java.io.IOException("File not found");
            }
        };

        // Act & Assert
        assertThatThrownBy(() -> new ModerationDictionaryValidation(badResource))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read JSON file");
    }
}
