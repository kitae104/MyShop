import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { POST_CONTENT_MAX, POST_TITLE_MAX, postApi, type Post } from '../api/posts.ts'
import { ApiError } from '../api/client.ts'
import { useAuth } from '../auth/AuthContext.tsx'
import FormField from '../components/FormField.tsx'

/** /posts/new 는 글쓰기, /posts/:id/edit 는 수정입니다. 수정은 게시글을 불러온 뒤 폼을 보여 줍니다. */
export default function PostFormPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const postId = id === undefined ? null : Number(id)
  const [loaded, setLoaded] = useState<{ id: number; post: Post } | null>(null)
  const [failure, setFailure] = useState<{ id: number; message: string } | null>(null)

  useEffect(() => {
    if (postId === null || !Number.isInteger(postId)) return
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

  if (postId === null) return <PostForm />

  const post = loaded?.id === postId ? loaded.post : null
  const error = !Number.isInteger(postId)
    ? '게시글을 찾을 수 없습니다.'
    : failure?.id === postId
      ? failure.message
      : post && user && user.id !== post.authorId
        ? '작성자만 게시글을 수정할 수 있습니다.'
        : null

  if (error) return <FormShell title="게시글 수정" backTo="/posts"><p className="text-sm text-red-600">{error}</p></FormShell>
  if (!post) return <FormShell title="게시글 수정" backTo="/posts"><p className="text-sm text-slate-500">불러오는 중...</p></FormShell>
  return <PostForm post={post} />
}

function FormShell({ title, backTo, children }: { title: string; backTo: string; children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <Link to={backTo} className="text-sm font-medium text-indigo-600 hover:underline">
        ← 돌아가기
      </Link>
      <h1 className="mt-4 text-2xl font-bold">{title}</h1>
      <div className="mt-6">{children}</div>
    </div>
  )
}

function PostForm({ post }: { post?: Post }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: post?.title ?? '', content: post?.content ?? '' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const update = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [key]: e.target.value })

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      const saved = post ? await postApi.update(post.id, form) : await postApi.create(form)
      navigate(`/posts/${saved.id}`, { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors(err.errors)
      } else {
        setError('요청을 처리하지 못했습니다.')
      }
      setSubmitting(false)
    }
  }

  return (
    <FormShell title={post ? '게시글 수정' : '글쓰기'} backTo={post ? `/posts/${post.id}` : '/posts'}>
      <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <FormField
          label="제목"
          name="title"
          required
          maxLength={POST_TITLE_MAX}
          value={form.title}
          onChange={update('title')}
          error={fieldErrors.title}
        />
        <div>
          <label htmlFor="content" className="mb-1 block text-sm font-medium text-slate-700">
            내용
          </label>
          <textarea
            id="content"
            name="content"
            required
            rows={12}
            maxLength={POST_CONTENT_MAX}
            value={form.content}
            onChange={update('content')}
            className={`w-full rounded-md border px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 ${
              fieldErrors.content ? 'border-red-400' : 'border-slate-300'
            }`}
          />
          {fieldErrors.content && <p className="mt-1 text-xs text-red-600">{fieldErrors.content}</p>}
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-indigo-600 py-2.5 font-medium text-white hover:bg-indigo-500 disabled:opacity-60"
        >
          {submitting ? '저장 중...' : post ? '수정하기' : '등록하기'}
        </button>
      </form>
    </FormShell>
  )
}
