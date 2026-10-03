import { Route, Routes } from 'react-router-dom'
import ProtectedRoute from './auth/ProtectedRoute.tsx'
import Layout from './components/Layout.tsx'
import DashboardPage from './pages/DashboardPage.tsx'
import LandingPage from './pages/LandingPage.tsx'
import LoginPage from './pages/LoginPage.tsx'
import NotFoundPage from './pages/NotFoundPage.tsx'
import PostDetailPage from './pages/PostDetailPage.tsx'
import PostFormPage from './pages/PostFormPage.tsx'
import PostListPage from './pages/PostListPage.tsx'
import SignupPage from './pages/SignupPage.tsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<LandingPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="posts" element={<PostListPage />} />
          <Route path="posts/new" element={<PostFormPage />} />
          <Route path="posts/:id" element={<PostDetailPage />} />
          <Route path="posts/:id/edit" element={<PostFormPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
