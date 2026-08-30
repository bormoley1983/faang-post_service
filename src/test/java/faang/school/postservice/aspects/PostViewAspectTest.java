package faang.school.postservice.aspects;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import faang.school.postservice.model.event.PostViewEvent;
import faang.school.postservice.publisher.post.PostViewPublisher;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostViewAspectTest {

    @Mock
    private PostViewPublisher postViewPublisher;

    @Mock
    private JoinPoint joinPoint;

    private PostViewAspect postViewAspect;

    @BeforeEach
    void setUp() {
        postViewAspect = new PostViewAspect(postViewPublisher);
        org.aspectj.lang.Signature signature = mock(org.aspectj.lang.Signature.class);
        org.mockito.Mockito.lenient().when(signature.getName()).thenReturn("testMethod");
        org.mockito.Mockito.lenient().when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    void publishEvent_whenResultNull_doesNotPublish() {
        // Act
        postViewAspect.publishEvent(joinPoint, null, null);

        // Assert
        verify(postViewPublisher, never()).publishEvent(PostViewEvent.class);
    }

    @Test
    void publishEvent_whenPostViewEvent_publishesToPostViewPublisher() {
        // Arrange
        PostViewEvent event = new PostViewEvent();

        // Act
        postViewAspect.publishEvent(joinPoint, null, event);

        // Assert
        verify(postViewPublisher).publishEvent(event);
    }

    @Test
    void publishEvent_whenUnknownResultType_delegatesToPublisherWithoutThrowing() {
        // Arrange
        Object unknownResult = new Object();

        // Act & Assert
        assertThatCode(() -> postViewAspect.publishEvent(joinPoint, null, unknownResult))
                .doesNotThrowAnyException();
    }
}
