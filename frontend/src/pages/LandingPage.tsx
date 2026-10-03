import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.tsx'

const features = [
  { title: 'Spring Boot', description: 'Java 21 기반 REST API와 Spring Security JWT 인증' },
  { title: 'React + Tailwind', description: 'Vite, TypeScript, Tailwind CSS로 구성된 프론트엔드' },
  { title: 'Docker Compose', description: 'PostgreSQL, 백엔드, 프론트엔드를 한 번에 실행' },
]

export default function LandingPage() {
  const { user } = useAuth()

  return (
    <div>
      <section className="mx-auto max-w-6xl px-4 py-24 text-center">
        <h1 className="text-4xl font-extrabold tracking-tight sm:text-5xl">
          새 프로젝트를 <span className="text-indigo-600">바로 시작</span>하세요
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg text-slate-600">
          로그인, 회원가입, 인증이 이미 준비된 템플릿입니다. 이제 서비스 기능에만 집중하면 됩니다.
        </p>
        <div className="mt-10 flex justify-center gap-4">
          {user ? (
            <Link to="/dashboard" className="rounded-md bg-indigo-600 px-5 py-3 font-medium text-white hover:bg-indigo-500">
              대시보드로 이동
            </Link>
          ) : (
            <>
              <Link to="/signup" className="rounded-md bg-indigo-600 px-5 py-3 font-medium text-white hover:bg-indigo-500">
                시작하기
              </Link>
              <Link to="/login" className="rounded-md border border-slate-300 px-5 py-3 font-medium hover:bg-white">
                로그인
              </Link>
            </>
          )}
        </div>
      </section>
      <section className="mx-auto grid max-w-6xl gap-6 px-4 pb-24 sm:grid-cols-3">
        {features.map((f) => (
          <div key={f.title} className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="font-semibold">{f.title}</h2>
            <p className="mt-2 text-sm text-slate-600">{f.description}</p>
          </div>
        ))}
      </section>
    </div>
  )
}
