package work.kitae104.myshop.post.dto;

import work.kitae104.myshop.post.Post;
import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorName,
        Instant createdAt,
        Instant updatedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
