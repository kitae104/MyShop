package work.kitae104.myshop.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 목록 API 공통 페이지 응답. page 는 0부터 시작합니다. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
