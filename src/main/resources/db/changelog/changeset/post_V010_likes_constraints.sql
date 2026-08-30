-- Preserve the earliest valid like when upgrading databases that already contain
-- rows written before target/uniqueness constraints existed.
DELETE FROM likes
WHERE (post_id IS NULL AND comment_id IS NULL)
   OR (post_id IS NOT NULL AND comment_id IS NOT NULL);

DELETE FROM likes duplicate
USING likes original
WHERE duplicate.id > original.id
  AND duplicate.user_id = original.user_id
  AND duplicate.post_id = original.post_id
  AND duplicate.post_id IS NOT NULL;

DELETE FROM likes duplicate
USING likes original
WHERE duplicate.id > original.id
  AND duplicate.user_id = original.user_id
  AND duplicate.comment_id = original.comment_id
  AND duplicate.comment_id IS NOT NULL;

ALTER TABLE likes
    ADD CONSTRAINT likes_exactly_one_target_chk
    CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL)
        OR (post_id IS NULL AND comment_id IS NOT NULL)
    );

CREATE UNIQUE INDEX likes_user_post_unique_idx
    ON likes (user_id, post_id)
    WHERE post_id IS NOT NULL;

CREATE UNIQUE INDEX likes_user_comment_unique_idx
    ON likes (user_id, comment_id)
    WHERE comment_id IS NOT NULL;
