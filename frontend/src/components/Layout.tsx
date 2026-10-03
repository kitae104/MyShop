import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.tsx'

const navClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm font-medium ${isActive ? 'text-indigo-600' : 'text-slate-600 hover:text-slate-900'}`

const YEAR = new Date().getFullYear()

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <Link to="/" className="text-lg font-bold text-indigo-600">
            Myshop
          </Link>
          <nav className="flex items-center gap-6">
            {user ? (
              <>
                <NavLink to="/dashboard" className={navClass}>
                  대시보드
                </NavLink>
                <NavLink to="/posts" className={navClass}>
                  게시판
                </NavLink>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-100"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={navClass}>
                  로그인
                </NavLink>
                <Link
                  to="/signup"
                  className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-500"
                >
                  회원가입
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        © {YEAR} Myshop
      </footer>
    </div>
  )
}
