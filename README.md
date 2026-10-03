# Myshop

로그인, 회원가입, 랜딩 페이지가 준비된 상태로 시작하는 프로젝트입니다. [dev-harness](https://github.com/kitae104/dev-harness) 템플릿으로 생성했습니다 (`.harness.json`).

## 구성

| 구분 | 기술 |
| --- | --- |
| backend | Spring Boot 4.1, Java 21, Gradle, Spring Security, JWT (jjwt), JPA |
| frontend | React 19, TypeScript, Vite, Tailwind CSS v4, React Router |
| db | PostgreSQL 18 |
| 실행 | Docker Compose |

## 바로 실행 (Docker Compose)

```bash
make up                # = docker compose up -d --build
make down              # = docker compose down  (DB 데이터는 유지)
```

- `.env` 는 생성할 때 임의의 `JWT_SECRET` 으로 이미 만들어져 있습니다. **`.env.example` 로 덮어쓰지 마세요** (공개된 기본 비밀값이 됩니다). `.env` 가 없을 때만 `cp .env.example .env` 후 `JWT_SECRET` 을 바꾸세요 (`openssl rand -base64 48`).
- `JWT_SECRET` 이 비어 있으면 `docker compose` 가 시작하지 않고 알려 줍니다.

- 프론트엔드: http://localhost:3000 (nginx 가 `/api` 를 백엔드로 프록시)
- 백엔드: http://localhost:8080 (`/actuator/health`)

### 다른 프로젝트와 함께 쓸 때

- 컨테이너(`<이름>-db`, `<이름>-backend`, `<이름>-frontend`), 네트워크(`<이름>-net`), 볼륨(`<이름>-db-data`)이 모두 `COMPOSE_PROJECT_NAME` 으로 시작합니다. 프로젝트마다 이름을 다르게 두면 서로 섞이지 않습니다.
- `make up` / `make down` 은 이 프로젝트의 컨테이너만 올리고 내립니다. 다른 프로젝트 컨테이너는 건드리지 않습니다.
- 동시에 여러 프로젝트를 띄우려면 `.env` 의 `DB_PORT`, `BACKEND_PORT`, `FRONTEND_PORT` 를 겹치지 않게 바꾸세요. 포트는 `127.0.0.1` 에만 열립니다.
- `make clean` 은 이 프로젝트의 DB 볼륨과 이미지까지 지웁니다 (데이터 초기화).
- `make` 가 없으면 주석에 적힌 `docker compose` 명령을 그대로 쓰면 됩니다.

## 로컬 개발

```bash
# 1) DB 만 컨테이너로
make db              # = docker compose up -d db

# 2) 백엔드 (http://localhost:8080)
cd backend && ./gradlew bootRun

# 3) 프론트엔드 (http://localhost:5173, /api 는 8080 으로 프록시)
cd frontend && npm install && npm run dev
```

- Windows(PowerShell/cmd)에서는 `./gradlew` 대신 `gradlew.bat` 을 씁니다. `make` 가 없으면 Makefile 주석의 `docker compose` 명령을 그대로 쓰면 됩니다.
- `./gradlew bootRun` 은 `.env` 를 읽지 않습니다. `.env` 에서 `DB_PORT` 를 바꿨다면 `DB_URL=jdbc:postgresql://localhost:<DB_PORT>/<DB_NAME>` 을 함께 지정해서 실행하세요.
- Gradle 실행에는 JDK 17 이상이 필요합니다. 컴파일용 JDK 21 이 없으면 Gradle 이 자동으로 내려받습니다 (foojay 툴체인).

## 검증 명령

```bash
cd backend && ./gradlew test        # H2 메모리 DB로 인증 API 통합 테스트
cd frontend && npm run lint && npm run build
```

## API

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | - | 회원가입 `{ email, password, name }` → 201 |
| POST | `/api/auth/login` | - | 로그인 `{ email, password }` → `{ accessToken, tokenType, expiresIn, user }` |
| GET | `/api/users/me` | Bearer | 내 정보 |
| GET | `/actuator/health` | - | 헬스 체크 |

에러 응답은 모두 `{ status, message, errors, timestamp }` 형식입니다 (`errors` 는 필드별 검증 메시지).

## 구조

```
backend/src/main/java/work/kitae104/myshop/
├── auth/        # 회원가입·로그인 (AuthController, AuthService, dto)
├── user/        # User 엔티티, 저장소, /api/users/me
├── security/    # JWT 발급·검증, 인증 필터, UserDetailsService
├── config/      # SecurityConfig, CORS 설정
└── common/      # ApiException, 전역 예외 처리

frontend/src/
├── api/         # fetch 래퍼(client.ts), 인증 API
├── auth/        # AuthContext(토큰 보관, 사용자 복원), ProtectedRoute
├── components/  # Layout(헤더/푸터), FormField
└── pages/       # Landing, Login, Signup, Dashboard(보호됨), NotFound
```

## 새 기능을 추가할 때

- 백엔드: 도메인별 패키지(`work.kitae104.myshop.<도메인>`)를 만들고 Controller → Service → Repository 순으로 둡니다. 인증이 필요 없는 경로는 `SecurityConfig` 의 `permitAll` 에 추가합니다.
- 프론트엔드: API 호출은 `src/api/<도메인>.ts` 에 두고 `api()` 래퍼를 사용합니다 (토큰 자동 첨부). 로그인이 필요한 페이지는 `App.tsx` 에서 `ProtectedRoute` 아래에 둡니다.

## 버전 기준

이 템플릿의 기준 버전입니다. 확장 모듈(예: Spring AI)은 이 버전에 맞춰 고릅니다.

- Spring Boot 4.1.1 (Spring Framework 7, Jackson 3), Java 21, Gradle 9.8
- PostgreSQL 18
- React 19, Vite 8, Tailwind CSS 4

## 참고

- 액세스 토큰만 사용하며 `localStorage` 에 저장합니다. 리프레시 토큰은 포함하지 않았습니다.
- 스키마는 `JPA_DDL_AUTO=update` 로 자동 생성됩니다. 운영 전에는 Flyway 같은 마이그레이션 도구로 바꾸는 것을 권장합니다.

## Claude Code 하네스

이 프로젝트에는 Claude Code 가 일관되게 기능을 추가하도록 돕는 설정이 들어 있습니다.

| 파일 | 역할 |
| --- | --- |
| `CLAUDE.md` | 구성, 명령, 작업 규칙 (Claude 가 매번 읽음) |
| `.claude/rules/*.md` | 폴더별 세부 규칙. 해당 파일을 다룰 때만 적용 (backend, frontend, docker …) |
| `.claude/settings.json` | 권한(.env 읽기 금지, 위험 명령은 확인)과 훅 |
| `.claude/hooks/lint-file.mjs` | 파일 수정 직후 그 파일만 린트 (oxlint, ruff) |
| `.claude/hooks/verify.mjs` | 작업을 끝낼 때 바뀐 영역의 테스트·린트 실행, 실패하면 Claude 가 이어서 고침 |
| `.claude/skills/add-domain` | `/add-domain 게시글(제목, 내용)` 처럼 도메인 기능을 백엔드~화면까지 추가 |
| `.claude/skills/verify` | `/verify` 전체 검증 후 표로 보고 |
| `.claude/agents/code-reviewer.md` | 규칙 기준 코드 리뷰 서브에이전트 |
| `.github/workflows/ci.yml` | GitHub Actions 에서 같은 검증 실행 |

- 훅은 Node 로 동작합니다 (Windows 는 Git Bash 필요). 검증 훅을 잠시 끄려면 `HARNESS_VERIFY=off claude`.
- 규칙을 바꾸고 싶으면 `CLAUDE.md` 나 `.claude/rules/` 를 직접 고치면 됩니다. 이후 Claude 의 작업에 바로 반영됩니다.
