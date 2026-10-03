import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { postApi, type Post } from '../api/posts.ts'
import { ApiError } from '../api/client.ts'
import { useAuth } from '../auth/AuthContext.tsx'

export default function PostDetailPage() {
  const { id } = useParams()
  const postId = Number(id)
  const { user } = useAuth()
  const navigate = useNavigate()
  const [loaded, setLoaded] = useState<{ id: number; post: Post } | null>(null)
  const [failure, setFailure] = useState<{ id: number; message: string } | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    if (!Number.isInteger(postId)) return
    let cancelled = false
    postApi
      .get(postId)
      .then((post) => {
        if (!cancelled) setLoaded({ id: postId, post })
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setFailure({ id: postId, message: err instanceof ApiError ? err.message : '요청을 처리하지 못했습니다.' })
        }
      })
    return () => {
      cancelled = true
    }
  }, [postId])

  const post = loaded?.id === postId ? loaded.post : null
  const error = !Number.isInteger(postId)
    ? '게시글을 찾을 수 없습니다.'
    : failure?.id === postId
      ? failure.message
      : null

  const handleDelete = async () => {
    if (!post || !window.confirm('이 게시글을 삭제할까요?')) return
    setActionError(null)
    setDeleting(true)
    try {
      await postApi.remove(post.id)
      navigate('/posts', { replace: true })
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : '요청을 처리하지 못했습니다.')
      setDeleting(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <Link to="/posts" className="text-sm font-medium text-indigo-600 hover:underline">
        ← 목록으로
      </Link>

      {error && <p className="mt-6 text-sm text-red-600">{error}</p>}
      {!post && !error && <p className="mt-6 text-sm text-slate-500">불러오는 중...</p>}

      {post && (
        <article className="mt-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <h1 className="text-2xl font-bold break-words">{post.title}</h1>
          <p className="mt-2 text-xs text-slate-500">
            {post.authorName} · {new Date(post.createdAt).toLocaleString('ko-KR')}
            {post.updatedAt !== post.createdAt && ` (수정 ${new Date(post.updatedAt).toLocaleString('ko-KR')})`}
          </p>
          <p className="mt-6 text-sm leading-relaxed break-words whitespace-pre-wrap">{post.content}</p>

          {user?.id === post.authorId && (
            <div className="mt-8 flex items-center gap-2 border-t border-slate-200 pt-4">
              <Link
                to={`/posts/${post.id}/edit`}
                className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-100"
              >
                수정
              </Link>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                className="rounded-md border border-red-300 px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 disabled:opacity-60"
              >
                {deleting ? '삭제 중...' : '삭제'}
              </button>
              {actionError && <p className="text-sm text-red-600">{actionError}</p>}
            </div>
          )}
        </article>
      )}
    </div>
  )
}
