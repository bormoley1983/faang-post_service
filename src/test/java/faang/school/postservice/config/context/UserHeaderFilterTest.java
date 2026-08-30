package faang.school.postservice.config.context;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class UserHeaderFilterTest {

    @Mock
    private FilterChain chain;

    @Mock
    private ServletResponse response;

    private final UserContext userContext = new UserContext();

    private final UserHeaderFilter filter = new UserHeaderFilter(userContext);

    @AfterEach
    void tearDown() {
        userContext.clear();
    }

    @Test
    void doFilter_whenUserIdHeaderPresent_setsUserContextForDownstream() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "42");
        AtomicLong userIdDuringChain = new AtomicLong(-1);

        // Act: use the real chain mock to capture context during downstream call
        org.mockito.Mockito.doAnswer(invocation -> {
            userIdDuringChain.set(userContext.getUserId());
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        // Assert: context was set during the chain and cleared afterwards
        assertThat(userIdDuringChain.get()).isEqualTo(42L);
        org.assertj.core.api.Assertions.assertThatThrownBy(userContext::getUserId)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void doFilter_whenNoUserIdHeader_doesNotSetUserContext() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicLong userIdDuringChain = new AtomicLong(-1);

        // Act
        org.mockito.Mockito.doAnswer(invocation -> {
            try {
                userIdDuringChain.set(userContext.getUserId());
            } catch (IllegalStateException e) {
                userIdDuringChain.set(-2);
            }
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        // Assert: context was never set
        org.assertj.core.api.Assertions.assertThat(userIdDuringChain.get()).isEqualTo(-2L);
    }

    @Test
    void doFilter_whenDownstreamThrows_stillClearsUserContext() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "42");

        org.mockito.Mockito.doThrow(new jakarta.servlet.ServletException("downstream failure"))
                .when(chain).doFilter(request, response);

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(jakarta.servlet.ServletException.class);

        // Context must be cleared even after downstream failure
        org.assertj.core.api.Assertions.assertThatThrownBy(userContext::getUserId)
                .isInstanceOf(IllegalStateException.class);
    }
}
