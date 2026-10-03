package work.kitae104.myshop.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createReadUpdateDeleteFlow() throws Exception {
        String token = signupAndLogin("홍길동");

        String body = mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "첫 글", "content", "안녕하세요"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andExpect(jsonPath("$.authorName").value("홍길동"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/posts/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("안녕하세요"));

        mockMvc.perform(put("/api/posts/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "수정된 글", "content", "내용 수정"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 글"))
                .andExpect(jsonPath("$.content").value("내용 수정"));

        mockMvc.perform(delete("/api/posts/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listIsLatestFirstAndPaginated() throws Exception {
        String token = signupAndLogin("목록");
        String marker = UUID.randomUUID().toString();
        for (int i = 1; i <= 3; i++) {
            createPost(token, marker + "-" + i, "내용 " + i);
        }

        mockMvc.perform(get("/api/posts").param("page", "0").param("size", "2").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value(marker + "-3"))
                .andExpect(jsonPath("$.content[1].title").value(marker + "-2"))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void otherUsersCanReadButNotModify() throws Exception {
        String owner = signupAndLogin("작성자");
        String other = signupAndLogin("타인");
        long id = createPost(owner, "내 글", "내용");

        mockMvc.perform(get("/api/posts/" + id).header("Authorization", bearer(other)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/posts/" + id)
                        .header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "가로채기", "content", "변경"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("작성자만 게시글을 수정할 수 있습니다."));

        mockMvc.perform(delete("/api/posts/" + id).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/posts/" + id).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.title").value("내 글"));
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        String token = signupAndLogin("검증");

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "", "content", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.content").exists());

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "가".repeat(101), "content", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("제목은 100자 이하여야 합니다."));

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "가".repeat(100), "content", "내용"))))
                .andExpect(status().isCreated());

        long id = createPost(token, "수정 대상", "내용");
        mockMvc.perform(put("/api/posts/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "", "content", "내용"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void contentOverLimitIsRejected() throws Exception {
        String token = signupAndLogin("긴내용");

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "제목", "content", "가".repeat(10001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.content").value("내용은 10000자 이하여야 합니다."));

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "제목", "content", "가".repeat(10000)))))
                .andExpect(status().isCreated());
    }

    @Test
    void titleAndContentAreTrimmed() throws Exception {
        String token = signupAndLogin("공백");

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "  제목  ", "content", "\n내용\n"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.content").value("내용"));
    }

    @Test
    void updateRefreshesUpdatedAtAndKeepsAuthor() throws Exception {
        String token = signupAndLogin("수정자");
        String created = mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "원본", "content", "내용"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode before = objectMapper.readTree(created);

        Thread.sleep(20);

        String updated = mockMvc.perform(put("/api/posts/" + before.get("id").asLong())
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "수정본", "content", "새 내용"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode after = objectMapper.readTree(updated);

        assertThat(after.get("authorId").asLong()).isEqualTo(before.get("authorId").asLong());
        // 작성 응답은 나노초, DB 에서 다시 읽은 값은 마이크로초라 밀리초로 맞춰 비교합니다.
        assertThat(instant(after, "createdAt")).isEqualTo(instant(before, "createdAt"));
        assertThat(instant(after, "updatedAt")).isAfter(instant(before, "updatedAt"));
    }

    @Test
    void pagingParametersAreValidatedAndClamped() throws Exception {
        String token = signupAndLogin("페이징");

        mockMvc.perform(get("/api/posts").param("page", "abc").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 값의 형식이 올바르지 않습니다."));
        mockMvc.perform(get("/api/posts").param("size", "abc").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/posts").param("page", "-1").param("size", "0").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));
        mockMvc.perform(get("/api/posts").param("size", "1000").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void unknownPostIsNotFound() throws Exception {
        String token = signupAndLogin("없음");

        mockMvc.perform(get("/api/posts/999999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
        mockMvc.perform(put("/api/posts/999999")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "제목", "content", "내용"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/posts/999999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestsWithoutTokenAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/posts/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "제목", "content", "내용"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "제목", "content", "내용"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/posts/1")).andExpect(status().isUnauthorized());
    }

    private long createPost(String token, String title, String content) throws Exception {
        String body = mockMvc.perform(post("/api/posts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "content", content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /** 테스트끼리 데이터가 겹치지 않도록 고유한 이메일로 가입하고 액세스 토큰을 돌려줍니다. */
    private String signupAndLogin(String name) throws Exception {
        String email = "post-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123", "name", name))))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("accessToken").asString();
    }

    private static Instant instant(JsonNode node, String field) {
        return Instant.parse(node.get(field).asString()).truncatedTo(ChronoUnit.MILLIS);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
