---
paths:
  - "frontend/**"
---

# 프론트엔드 규칙 (React 19, TypeScript, Tailwind v4)

- API 호출은 `src/api/<도메인>.ts` 에 `xxxApi` 객체로 모으고 반드시 `client.ts` 의 `api<T>()` 를 거칩니다. 컴포넌트에서 `fetch` 를 직접 쓰지 않습니다 (토큰, 에러 형식 처리가 여기 있음).
- 서버 응답 타입은 `src/api/<도메인>.ts` 에 `interface` 로 선언하고 백엔드 DTO 와 필드 이름을 맞춥니다.
- 에러 표시: `catch (err) { err instanceof ApiError ? err.message : '요청을 처리하지 못했습니다.' }`. 필드별 메시지는 `err.errors[필드]` 로 `FormField` 의 `error` 에 넘깁니다 (`pages/SignupPage.tsx` 참고).
- 페이지는 `src/pages/XxxPage.tsx`(default export), 재사용 UI 는 `src/components/`. 로그인이 필요한 화면은 `App.tsx` 의 `<ProtectedRoute>` 안에 라우트를 추가하고, 메뉴는 `components/Layout.tsx` 에 추가합니다.
- 로그인 사용자: `useAuth()` (`auth/AuthContext.tsx`). 토큰을 직접 다루지 않습니다. 토큰 만료(401)는 `client.ts` 가 토큰을 지우고 `AuthContext` 가 로그아웃 상태로 바꾸므로 페이지에서 따로 처리하지 않습니다.
- 스타일은 Tailwind 유틸리티 클래스만 씁니다. 기본 색은 `indigo-600`(주), `slate-*`(본문/테두리), 카드 `rounded-xl border border-slate-200 bg-white p-6 shadow-sm`.
- 화면 문구는 한국어.
- import 경로에 확장자(`.tsx`, `.ts`)를 붙입니다 (기존 코드와 동일).
- 검증: `cd frontend && npm run lint && npm run build`
