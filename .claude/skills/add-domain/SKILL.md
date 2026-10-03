---
name: add-domain
description: 새 도메인(예: 게시글, 상품) 기능을 백엔드 API부터 프론트엔드 화면까지 한 번에 추가한다. "게시판 만들어줘", "상품 CRUD 추가" 같은 요청에 사용.
argument-hint: "<도메인 이름과 필드 설명>"
---

# 도메인 기능 추가

요청: $ARGUMENTS

기존 `user/`, `auth/` 패키지와 `pages/` 를 기준으로, 같은 모양의 코드를 세로로 한 번에 만듭니다. 필드나 권한이 요청에 없으면 합리적인 기본값을 정하고 마지막 보고에 적습니다.

## 1. 설계 확인 (코드 작성 전)
- 엔티티 이름(영문 단수, 예: `Post`), 필드와 타입, 필수 여부, 소유자(작성 사용자) 연결 여부.
- 엔드포인트: 기본은 `GET /api/<복수>`(목록, 페이지네이션 `page`,`size`), `GET /api/<복수>/{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`.
- 권한: 기본은 로그인 사용자 전체 조회, 수정·삭제는 작성자만 (아니면 403).

## 2. 백엔드 (`backend/src/main/java/<패키지>/<도메인>/`)
1. `Xxx.java` 엔티티 (+ 작성자면 `@ManyToOne(fetch = LAZY) User owner`)
2. `XxxRepository.java` (`JpaRepository<Xxx, Long>`)
3. `dto/XxxCreateRequest.java`, `dto/XxxUpdateRequest.java` (검증 + 한국어 메시지), `dto/XxxResponse.java` (`from`)
4. `XxxService.java` (없으면 `ApiException(NOT_FOUND, ...)`, 권한 없으면 `FORBIDDEN`)
5. `XxxController.java`
6. 테스트 `backend/src/test/java/<패키지>/<도메인>/XxxControllerTest.java`: 생성→조회→수정→삭제 흐름, 401, 400, 403, 404
7. `cd backend && ./gradlew test` 통과 확인

## 3. 프론트엔드 (`frontend/src/`)
1. `api/<도메인>.ts`: 응답 `interface` + `xxxApi` (모두 `api<T>()` 사용)
2. `pages/XxxListPage.tsx`, 필요하면 `XxxFormPage.tsx` / `XxxDetailPage.tsx` (기존 페이지의 카드·폼 스타일, `FormField` 재사용)
3. `App.tsx` 의 `<ProtectedRoute>` 안에 라우트, `components/Layout.tsx` 에 메뉴 링크
4. `cd frontend && npm run lint && npm run build` 통과 확인

## 4. 마무리
- 새 환경 변수가 생겼다면 `.env.example`, `docker-compose.yml`, `application.yml` 에 함께 반영
- `README.md` 의 API 표에 새 엔드포인트 추가
- 보고: 만든 파일, 엔드포인트 표, 정한 기본값(권한, 필드), 실행한 검증과 결과
