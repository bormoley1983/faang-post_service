package faang.school.postservice.repository;

import faang.school.postservice.model.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostCacheRepository {
    private static final String POST_PREFIX = "post:";

    private final RedisTemplate<String, Post> redisTemplate;

    public void save(Post post, Duration ttl) {
        String key = POST_PREFIX + post.getId();
        redisTemplate.opsForValue().set(key, post, ttl);
    }

    public Optional<Post> findById(Long id) {
        String key = POST_PREFIX + id;
        Post post = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(post);
    }

    public void deleteById(Long id) {
        String key = POST_PREFIX + id;
        redisTemplate.delete(key);
    }

    public void removeExpiredPosts() {
        log.debug("Redis automatically handles expiration based on TTL");

        Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(POST_PREFIX + "*").count(100).build());

        List<String> keysToDelete = new ArrayList<>();

        try {
            while (cursor.hasNext()) {
                String key = cursor.next();
                long ttl = redisTemplate.getExpire(key);

                if (ttl >= 0 && ttl < 300) {
                    keysToDelete.add(key);
                }
            }
        } finally {
            cursor.close();
        }

        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
            log.info("Proactively removed {} soon-to-expire posts", keysToDelete.size());
        }
    }
}
