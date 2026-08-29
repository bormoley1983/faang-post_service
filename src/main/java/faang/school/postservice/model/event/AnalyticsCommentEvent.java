package faang.school.postservice.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AnalyticsCommentEvent implements Event {
    private int schemaVersion = 1;
    private String eventId;
    private long postId;
    private long authorId;
    private long commentId;
    private Instant timestamp;
}
