---
paths:
  - "backend/**"
---

# 백엔드 규칙 (Spring Boot 4.1, Java 21)

## 구조
- 기능(도메인)별 패키지: `<기본 패키지>/<도메인>/` 안에 `Entity`, `Repository`, `Service`, `Controller`, `dto/` 를 둡니다. 계층별 패키지(`controller/`, `service/`)로 나누지 않습니다.
- 공통: `common/`(ApiException, ErrorResponse, 전역 예외 처리) · `config/`(보안, CORS) · `security/`(JWT).

## 코드
- DTO 는 `record`. 요청 DTO 에는 Bean Validation 과 한국어 `message` 를 붙입니다 (`auth/dto/SignupRequest.java` 참고).
- 응답 DTO 는 `XxxResponse.from(entity)` 정적 팩토리로 만듭니다. 엔티티를 컨트롤러 밖으로 그대로 반환하지 않습니다.
- 엔티티: Lombok `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, 생성은 `@Builder` 또는 정적 팩토리. `@Setter` 는 쓰지 않고 의미 있는 변경 메서드를 둡니다.
- 의존성 주입은 생성자 주입(`@RequiredArgsConstructor` + `private final`). 필드 `@Autowired` 금지.
- 서비스 트랜잭션은 메서드 단위: 조회 `@Transactional(readOnly = true)`, 변경 `@Transactional` (`auth/AuthService.java` 참고).
- 실패는 `throw new ApiException(HttpStatus.XXX, "사용자에게 보여줄 한국어 메시지")`. `GlobalExceptionHandler` 가 Spring 기본 예외(잘못된 JSON 400, 타입 불일치 400, 404, 405, 403, 무결성 위반 409)와 나머지(500, 로그만 남기고 내부 정보 숨김)를 같은 `ErrorResponse` 형식으로 바꿉니다. 새 예외 타입이 필요하면 여기에 처리기를 추가합니다.
- 현재 사용자: 컨트롤러 파라미터 `@AuthenticationPrincipal UserDetails principal` (`principal.getUsername()` 은 이메일).
- JSON 은 Jackson 3 (`tools.jackson.*`). `com.fasterxml.jackson.databind` 를 import 하지 않습니다.
- 공개 API 를 추가하면 `SecurityConfig` 의 `permitAll` 목록에 명시적으로 넣습니다. 기본은 인증 필요.
- 설정값은 `@ConfigurationProperties` record (`security/JwtProperties.java` 참고) + `application.yml` 에 `${ENV:기본값}`.

## 테스트
- 컨트롤러 단위가 아니라 `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")` 통합 테스트로 HTTP 계약을 검증합니다 (H2, `application-test.yml`).
- 엔드포인트마다 정상, 인증 없음(401), 검증 실패(400), 주요 실패(404/409) 를 확인합니다.
- 테스트끼리 데이터가 겹치지 않게 이메일 등은 고유하게 만듭니다.
- 실행: `cd backend && ./gradlew test`

## DB
- 지금은 `ddl-auto: update` 입니다. 운영 배포 전에는 Flyway 를 도입하고 `validate` 로 바꿉니다 (도입 시 이 문서를 갱신).
