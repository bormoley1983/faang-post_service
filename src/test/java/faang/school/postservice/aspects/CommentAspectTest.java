package faang.school.postservice.aspects;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faang.school.postservice.annotations.PublishCommentEvent;
import faang.school.postservice.model.event.AnalyticsCommentEvent;
import faang.school.postservice.model.event.Event;
import faang.school.postservice.model.event.NotificationCommentEvent;
import faang.school.postservice.publisher.comment.AnalyticsCommentEventPublisher;
import faang.school.postservice.publisher.comment.NotificationCommentEventPublisher;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentAspectTest {

    @Mock
    private AnalyticsCommentEventPublisher analyticsCommentEventPublisher;

    @Mock
    private NotificationCommentEventPublisher notificationCommentEventPublisher;

    @Mock
    private JoinPoint joinPoint;

    private CommentAspect commentAspect;

    @BeforeEach
    void setUp() {
        commentAspect = new CommentAspect(analyticsCommentEventPublisher, notificationCommentEventPublisher);
        org.aspectj.lang.Signature signature = mock(org.aspectj.lang.Signature.class);
        org.mockito.Mockito.lenient().when(signature.getName()).thenReturn("testMethod");
        org.mockito.Mockito.lenient().when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    void publishEvent_whenResultNull_doesNotPublish() {
        // Arrange: annotation is not accessed when result is null
        PublishCommentEvent annotation = mock(PublishCommentEvent.class);

        // Act
        commentAspect.publishEvent(joinPoint, annotation, null);

        // Assert
        verify(analyticsCommentEventPublisher, never()).publishEvent(AnalyticsCommentEvent.class);
        verify(notificationCommentEventPublisher, never()).publishEvent(NotificationCommentEvent.class);
    }

    @Test
    void publishEvent_whenAnalyticsEventClass_publishesOnlyToAnalytics() {
        // Arrange
        AnalyticsCommentEvent event = new AnalyticsCommentEvent();
        PublishCommentEvent annotation = mockAnnotation(AnalyticsCommentEvent.class);

        // Act
        commentAspect.publishEvent(joinPoint, annotation, event);

        // Assert
        verify(analyticsCommentEventPublisher).publishEvent(event);
        verify(notificationCommentEventPublisher, never()).publishEvent(NotificationCommentEvent.class);
    }

    @Test
    void publishEvent_whenNotificationEventClass_publishesOnlyToNotification() {
        // Arrange
        NotificationCommentEvent event = new NotificationCommentEvent();
        PublishCommentEvent annotation = mockAnnotation(NotificationCommentEvent.class);

        // Act
        commentAspect.publishEvent(joinPoint, annotation, event);

        // Assert
        verify(notificationCommentEventPublisher).publishEvent(event);
        verify(analyticsCommentEventPublisher, never()).publishEvent(AnalyticsCommentEvent.class);
    }

    @Test
    void publishEvent_whenBaseEventClass_publishesToBothPublishers() {
        // Arrange
        AnalyticsCommentEvent event = new AnalyticsCommentEvent();
        PublishCommentEvent annotation = mockAnnotation(Event.class);

        // Act
        commentAspect.publishEvent(joinPoint, annotation, event);

        // Assert
        InOrder inOrder = inOrder(analyticsCommentEventPublisher, notificationCommentEventPublisher);
        inOrder.verify(analyticsCommentEventPublisher).publishEvent(event);
        inOrder.verify(notificationCommentEventPublisher).publishEvent(event);
    }

    @Test
    void publishEvent_whenMultipleEventClasses_publishesToAllMatchingPublishers() {
        // Arrange
        AnalyticsCommentEvent analyticsEvent = new AnalyticsCommentEvent();
        NotificationCommentEvent notificationEvent = new NotificationCommentEvent();
        PublishCommentEvent annotation = mockAnnotation(AnalyticsCommentEvent.class, NotificationCommentEvent.class);

        // Act
        commentAspect.publishEvent(joinPoint, annotation, analyticsEvent);
        commentAspect.publishEvent(joinPoint, annotation, notificationEvent);

        // Assert
        verify(analyticsCommentEventPublisher).publishEvent(analyticsEvent);
        verify(notificationCommentEventPublisher).publishEvent(notificationEvent);
    }

    @Test
    void publishEvent_whenUnknownResultType_doesNotThrow() {
        // Arrange
        PublishCommentEvent annotation = mockAnnotation(AnalyticsCommentEvent.class);
        Object unknownResult = new Object();

        // Act & Assert
        assertThatCode(() -> commentAspect.publishEvent(joinPoint, annotation, unknownResult))
                .doesNotThrowAnyException();
    }

    private PublishCommentEvent mockAnnotation(Class<? extends Event>... eventClasses) {
        PublishCommentEvent annotation = mock(PublishCommentEvent.class);
        when(annotation.events()).thenReturn(eventClasses);
        return annotation;
    }
}
