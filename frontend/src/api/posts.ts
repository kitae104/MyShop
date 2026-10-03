import { api } from './client.ts'

// 백엔드 Post.TITLE_MAX / CONTENT_MAX 와 같은 값이어야 합니다.
export const POST_TITLE_MAX = 100
export const POST_CONTENT_MAX = 10000

export interface Post {
  id: number
  title: string
  content: string
  authorId: number
  authorName: string
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PostInput {
  title: string
  content: string
}

export const postApi = {
  list: (page = 0, size = 10) => api<PageResponse<Post>>(`/api/posts?page=${page}&size=${size}`),
  get: (id: number) => api<Post>(`/api/posts/${id}`),
  create: (input: PostInput) => api<Post>('/api/posts', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: number, input: PostInput) =>
    api<Post>(`/api/posts/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  remove: (id: number) => api<void>(`/api/posts/${id}`, { method: 'DELETE' }),
}
