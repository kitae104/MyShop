package work.kitae104.myshop.post.dto;

import work.kitae104.myshop.post.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 작성·수정 공용 요청. 길이 상한은 엔티티 컬럼 길이와 같은 상수를 씁니다. */
public record PostRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = Post.TITLE_MAX, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = Post.CONTENT_MAX, message = "내용은 10000자 이하여야 합니다.")
        String content) {
}
