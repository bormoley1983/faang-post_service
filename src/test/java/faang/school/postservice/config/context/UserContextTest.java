package faang.school.postservice.config.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserContextTest {

    private final UserContext userContext = new UserContext();

    @AfterEach
    void tearDown() {
        userContext.clear();
    }

    @Test
    void getUserId_whenUserIdSet_returnsUserId() {
        // Arrange
        userContext.setUserId(42L);

        // Act & Assert
        assertThat(userContext.getUserId()).isEqualTo(42L);
    }

    @Test
    void getUserId_whenNoUserIdSet_throwsIllegalStateException() {
        // Act & Assert
        assertThatThrownBy(userContext::getUserId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user is required");
    }

    @Test
    void clear_removesStoredUserId() {
        // Arrange
        userContext.setUserId(42L);

        // Act
        userContext.clear();

        // Assert
        assertThatThrownBy(userContext::getUserId)
                .isInstanceOf(IllegalStateException.class);
    }
}
