# 프로젝트 안내 (Claude Code)

dev-harness 템플릿으로 만든 프로젝트입니다. 어떤 템플릿과 확장 모듈로 만들었는지는 `.harness.json` 에 있습니다.

## 구성

| 폴더 | 내용 |
| --- | --- |
| `backend/` | Spring Boot 4.1, Java 21, Gradle, Spring Security + JWT, JPA, PostgreSQL 18 |
| `frontend/` | React 19, TypeScript, Vite, Tailwind CSS v4, React Router |
| `docker-compose.yml` | 이 프로젝트 전용 Compose 스택 (이름은 `.env` 의 `COMPOSE_PROJECT_NAME`) |

## 명령

```bash
make up / make down / make ps / make logs   # 이 프로젝트 컨테이너만 대상
make db                                      # 로컬 개발용 DB 만 실행
cd backend && ./gradlew test                 # 백엔드 테스트 (H2)
cd frontend && npm run lint && npm run build # 프론트엔드 린트 + 타입 검사 + 빌드
```

## 작업 규칙

1. **작업이 끝나기 전에 검증합니다.** 바꾼 영역의 테스트·린트·빌드를 실행하고 통과를 확인한 뒤 완료라고 말합니다. Stop 훅(`.claude/hooks/verify.mjs`)이 바뀐 영역을 자동으로 검사하고, 실패하면 고쳐야 끝낼 수 있습니다.
2. **기능은 세로로 자릅니다.** API 하나를 추가하면 백엔드(엔티티 → 저장소 → 서비스 → 컨트롤러 → 테스트)와 프론트엔드(`src/api/*.ts` → 페이지 → 라우트)를 같은 작업에서 끝냅니다. 절차는 `/add-domain` 스킬에 있습니다.
3. **기존 패턴을 따릅니다.** 새 코드를 쓰기 전에 같은 종류의 기존 파일(`auth/`, `user/`, `pages/LoginPage.tsx` 등)을 읽고 구조, 이름, 에러 처리를 맞춥니다. 폴더별 세부 규칙은 `.claude/rules/` 에 있고 해당 파일을 다룰 때 자동으로 적용됩니다.
4. **에러 응답 형식은 하나입니다.** 모든 API 에러는 `{ status, message, errors, timestamp }` 이고 메시지는 사용자에게 보여줄 한국어 문장입니다.
5. **비밀값은 `.env` 에만 둡니다.** `.env` 는 읽거나 커밋하지 않습니다. 새 설정은 `.env.example`, `docker-compose.yml`, `application.yml`(기본값 포함)에 함께 추가합니다.
6. **Docker 이름 규칙을 지킵니다.** 새 서비스의 `container_name`, `image`, 볼륨·네트워크 `name` 은 `${COMPOSE_PROJECT_NAME}-` 로 시작하고, 포트는 `127.0.0.1:${XXX_PORT:-기본값}` 으로 엽니다. 같은 Docker 에 다른 프로젝트가 함께 돌아갑니다.
7. **되돌리기 어려운 명령은 먼저 묻습니다.** `make clean`, `docker compose down -v`, `git push`, DB 데이터 삭제.

## 확장 모듈
