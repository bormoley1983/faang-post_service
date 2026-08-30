package faang.school.postservice.exception;

/**
 * Thrown when a Kafka event cannot be serialized to JSON.
 */
public class EventSerializationException extends RuntimeException {

    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
