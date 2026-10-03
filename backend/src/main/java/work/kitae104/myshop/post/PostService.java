package work.kitae104.myshop.post;

import work.kitae104.myshop.common.ApiException;
import work.kitae104.myshop.common.PageResponse;
import work.kitae104.myshop.post.dto.PostRequest;
import work.kitae104.myshop.post.dto.PostResponse;
import work.kitae104.myshop.user.User;
import work.kitae104.myshop.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> list(int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), LATEST_FIRST);
        return PageResponse.from(postRepository.findAll(pageable), PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return PostResponse.from(find(id));
    }

    @Transactional
    public PostResponse create(String email, PostRequest request) {
        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."));
        Post post = Post.builder()
                .title(request.title().trim())
                .content(request.content().trim())
                .author(author)
                .build();
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public PostResponse update(String email, Long id, PostRequest request) {
        Post post = findOwned(email, id, "작성자만 게시글을 수정할 수 있습니다.");
        post.update(request.title().trim(), request.content().trim());
        // updatedAt 은 @PreUpdate 에서 갱신되므로 flush 이후 응답을 만듭니다.
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(String email, Long id) {
        postRepository.delete(findOwned(email, id, "작성자만 게시글을 삭제할 수 있습니다."));
    }

    private Post find(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private Post findOwned(String email, Long id, String forbiddenMessage) {
        Post post = find(id);
        if (!post.isWrittenBy(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
        return post;
    }
}
