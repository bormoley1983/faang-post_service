package faang.school.postservice.repository;

import faang.school.postservice.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByPostIdOrderByCreatedAtAsc(long postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.verified IS NULL")
    List<Comment> findUnverifiedComments();

    @Query("SELECT c.id FROM Comment c WHERE c.verified IS NULL ORDER BY c.id")
    List<Long> findUnverifiedCommentIds(Pageable pageable);

    List<Comment> findAllByIdIn(List<Long> ids);

    @Query(nativeQuery = true, value = """
            SELECT author_id 
            FROM comment 
            WHERE verified = false AND verified_date IS NOT NULL
            GROUP BY author_id
            HAVING COUNT(*) >= :unverifiedCommentsCountForBan
            """
    )
    List<Long> findAuthorsForBanWithUnverifiedCommentsCount(int unverifiedCommentsCountForBan);
}
