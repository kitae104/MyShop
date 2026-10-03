import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { postApi, type PageResponse, type Post } from '../api/posts.ts'
import { ApiError } from '../api/client.ts'

const PAGE_SIZE = 10

export default function PostListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  // 주소의 page 는 1부터, API 의 page 는 0부터 시작합니다.
  const page = Math.max((Number(searchParams.get('page')) || 1) - 1, 0)
  const [result, setResult] = useState<{ page: number; data: PageResponse<Post> } | null>(null)
  const [failure, setFailure] = useState<{ page: number; message: string } | null>(null)

  useEffect(() => {
    let cancelled = false
    postApi
      .list(page, PAGE_SIZE)
      .then((data) => {
        if (!cancelled) setResult({ page, data })
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setFailure({ page, message: err instanceof ApiError ? err.message : '요청을 처리하지 못했습니다.' })
        }
      })
    return () => {
      cancelled = true
    }
  }, [page])

  const data = result?.page === page ? result.data : null
  const error = failure?.page === page ? failure.message : null

  const goTo = (target: number) => setSearchParams(target === 0 ? {} : { page: String(target + 1) })

  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">게시판</h1>
        <Link
          to="/posts/new"
          className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-indigo-500"
        >
          글쓰기
        </Link>
      </div>

      {error && <p className="mt-6 text-sm text-red-600">{error}</p>}
      {!data && !error && <p className="mt-6 text-sm text-slate-500">불러오는 중...</p>}

      {data && data.content.length === 0 && (
        <p className="mt-6 rounded-xl border border-slate-200 bg-white p-6 text-center text-sm text-slate-500 shadow-sm">
          {data.totalPages > 0 ? '이 페이지에는 게시글이 없습니다.' : '아직 게시글이 없습니다. 첫 글을 작성해 보세요.'}
        </p>
      )}

      {data && data.content.length > 0 && (
        <ul className="mt-6 divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white shadow-sm">
          {data.content.map((post) => (
            <li key={post.id}>
              <Link to={`/posts/${post.id}`} className="block px-6 py-4 hover:bg-slate-50">
                <p className="font-medium">{post.title}</p>
                <p className="mt-1 text-xs text-slate-500">
                  {post.authorName} · {new Date(post.createdAt).toLocaleString('ko-KR')}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-4 text-sm">
          <button
            type="button"
            onClick={() => goTo(page - 1)}
            disabled={page === 0}
            className="rounded-md border border-slate-300 px-3 py-1.5 font-medium hover:bg-slate-100 disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-slate-600">
            {data.page + 1} / {data.totalPages}
          </span>
          <button
            type="button"
            onClick={() => goTo(page + 1)}
            disabled={page + 1 >= data.totalPages}
            className="rounded-md border border-slate-300 px-3 py-1.5 font-medium hover:bg-slate-100 disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  )
}
