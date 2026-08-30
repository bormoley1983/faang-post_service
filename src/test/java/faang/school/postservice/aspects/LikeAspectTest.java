package faang.school.postservice.aspects;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faang.school.postservice.annotations.PublishLikeEvent;
import faang.school.postservice.model.event.AnalyticsLikeEvent;
import faang.school.postservice.model.event.Event;
import faang.school.postservice.model.event.NotificationLikeEvent;
import faang.school.postservice.publisher.like.AnalyticsLikeEventPublisher;
import faang.school.postservice.publisher.like.NotificationLikeEventPublisher;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeAspectTest {

    @Mock
    private AnalyticsLikeEventPublisher analyticsLikeEventPublisher;

    @Mock
    private NotificationLikeEventPublisher notificationLikeEventPublisher;

    @Mock
    private JoinPoint joinPoint;

    private LikeAspect likeAspect;

    @BeforeEach
    void setUp() {
        likeAspect = new LikeAspect(analyticsLikeEventPublisher, notificationLikeEventPublisher);
        org.aspectj.lang.Signature signature = mock(org.aspectj.lang.Signature.class);
        org.mockito.Mockito.lenient().when(signature.getName()).thenReturn("testMethod");
        org.mockito.Mockito.lenient().when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    void publishEvent_whenResultNull_doesNotPublish() {
        // Arrange: annotation is not accessed when result is null
        PublishLikeEvent annotation = mock(PublishLikeEvent.class);

        // Act
        likeAspect.publishEvent(joinPoint, annotation, null);

        // Assert
        verify(analyticsLikeEventPublisher, never()).publishEvent(AnalyticsLikeEvent.class);
        verify(notificationLikeEventPublisher, never()).publishEvent(NotificationLikeEvent.class);
    }

    @Test
    void publishEvent_whenAnalyticsEventClass_publishesOnlyToAnalytics() {
        // Arrange
        AnalyticsLikeEvent event = new AnalyticsLikeEvent();
        PublishLikeEvent annotation = mockAnnotation(AnalyticsLikeEvent.class);

        // Act
        likeAspect.publishEvent(joinPoint, annotation, event);

        // Assert
        verify(analyticsLikeEventPublisher).publishEvent(event);
        verify(notificationLikeEventPublisher, never()).publishEvent(NotificationLikeEvent.class);
    }

    @Test
    void publishEvent_whenNotificationEventClass_publishesOnlyToNotification() {
        // Arrange
        NotificationLikeEvent event = new NotificationLikeEvent();
        PublishLikeEvent annotation = mockAnnotation(NotificationLikeEvent.class);

        // Act
        likeAspect.publishEvent(joinPoint, annotation, event);

        // Assert
        verify(notificationLikeEventPublisher).publishEvent(event);
        verify(analyticsLikeEventPublisher, never()).publishEvent(AnalyticsLikeEvent.class);
    }

    @Test
    void publishEvent_whenBaseEventClass_publishesToBothPublishers() {
        // Arrange
        AnalyticsLikeEvent event = new AnalyticsLikeEvent();
        PublishLikeEvent annotation = mockAnnotation(Event.class);

        // Act
        likeAspect.publishEvent(joinPoint, annotation, event);

        // Assert
        InOrder inOrder = inOrder(analyticsLikeEventPublisher, notificationLikeEventPublisher);
        inOrder.verify(analyticsLikeEventPublisher).publishEvent(event);
        inOrder.verify(notificationLikeEventPublisher).publishEvent(event);
    }

    @Test
    void publishEvent_whenUnknownResultType_doesNotThrow() {
        // Arrange
        PublishLikeEvent annotation = mockAnnotation(AnalyticsLikeEvent.class);
        Object unknownResult = new Object();

        // Act & Assert
        assertThatCode(() -> likeAspect.publishEvent(joinPoint, annotation, unknownResult))
                .doesNotThrowAnyException();
    }

    private PublishLikeEvent mockAnnotation(Class<? extends Event>... eventClasses) {
        PublishLikeEvent annotation = mock(PublishLikeEvent.class);
        when(annotation.events()).thenReturn(eventClasses);
        return annotation;
    }
}
