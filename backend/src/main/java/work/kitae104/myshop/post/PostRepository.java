package work.kitae104.myshop.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 목록에서 작성자 이름을 N+1 쿼리 없이 가져오기 위해 author 를 함께 조회합니다. */
    @Override
    @EntityGraph(attributePaths = "author")
    Page<Post> findAll(Pageable pageable);
}
