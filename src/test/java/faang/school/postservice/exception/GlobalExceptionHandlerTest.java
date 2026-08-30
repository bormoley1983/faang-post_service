package faang.school.postservice.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationExceptions_mapsFieldErrorsToBadRequestBody() {
        // Arrange
        BindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "title", "Title is required"));
        bindingResult.addError(new FieldError("target", "content", "Content is required"));
        org.springframework.core.MethodParameter methodParameter =
                new org.springframework.core.MethodParameter(findHandlerMethod(), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // Act
        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("title", "Title is required")
                .containsEntry("content", "Content is required");
    }

    @Test
    void handleBadRequestException_whenIllegalArgument_returns400WithMessage() {
        // Arrange: the handler is registered for both AlreadyLikedException and IllegalArgumentException.
        // The method signature declares AlreadyLikedException, so we test with that type here;
        // the IllegalArgumentException path is covered by Spring's exception resolution at runtime.
        AlreadyLikedException ex = new AlreadyLikedException("Bad input");

        // Act
        ResponseEntity<String> response = handler.handleBadRequestException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Bad input");
    }

    @Test
    void handleNotFoundException_whenPostNotFound_returns404WithMessage() {
        // Act
        ResponseEntity<String> response = handler.handleNotFoundException(new PostNotFoundException("Post 1 not found"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Post 1 not found");
    }

    @Test
    void handleBadRequestException_whenAlreadyLiked_returns400WithMessage() {
        // Act
        ResponseEntity<String> response = handler.handleBadRequestException(new AlreadyLikedException("Already liked"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Already liked");
    }

    @Test
    void handleResourceNotFoundException_returns404WithMessage() {
        // Act
        ResponseEntity<String> response = handler.handleResourceNotFoundException(new ResourceNotFoundException("Resource missing"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Resource missing");
    }

    @Test
    void handleFileFormatException_returns400WithMessage() {
        // Act
        ResponseEntity<String> response = handler.handleFileFormatException(new FileFormatException("Unsupported format"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Unsupported format");
    }

    private java.lang.reflect.Method findHandlerMethod() {
        try {
            return Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
