package faang.school.postservice.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsLikeEvent implements Event {
    private int schemaVersion = 1;
    private String eventId;
    private Long postId;
    private Long userId;
    private Long authorId;
    private Instant timestamp;
}
