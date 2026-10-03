import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="py-24 text-center">
      <p className="text-5xl font-bold text-slate-300">404</p>
      <p className="mt-4 text-slate-600">페이지를 찾을 수 없습니다.</p>
      <Link to="/" className="mt-6 inline-block font-medium text-indigo-600 hover:underline">
        홈으로
      </Link>
    </div>
  )
}
