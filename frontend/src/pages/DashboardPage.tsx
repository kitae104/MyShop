import { useAuth } from '../auth/AuthContext.tsx'

export default function DashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="text-2xl font-bold">안녕하세요, {user.name}님</h1>
      <p className="mt-2 text-slate-600">로그인에 성공했습니다. 이 페이지부터 서비스 기능을 추가하세요.</p>
      <dl className="mt-8 grid max-w-lg grid-cols-3 gap-y-3 rounded-xl border border-slate-200 bg-white p-6 text-sm shadow-sm">
        <dt className="text-slate-500">이메일</dt>
        <dd className="col-span-2">{user.email}</dd>
        <dt className="text-slate-500">권한</dt>
        <dd className="col-span-2">{user.role}</dd>
        <dt className="text-slate-500">가입일</dt>
        <dd className="col-span-2">{new Date(user.createdAt).toLocaleDateString('ko-KR')}</dd>
      </dl>
    </div>
  )
}
