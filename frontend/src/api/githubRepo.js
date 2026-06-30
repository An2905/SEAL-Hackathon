import { apiFetch } from './client'

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

export function parseGitHubRepoUrl(url) {
  const match = String(url || '').match(/github\.com\/([^/]+)\/([^/\s]+?)(?:\.git)?\/?$/)
  if (!match) return null
  return { owner: match[1], repo: match[2] }
}

// GET /api/github/repos/{owner}/{repo}/commits
export async function listCommits({ owner, repo, sha, author, since, until, perPage = 20, page = 1 } = {}) {
  const params = new URLSearchParams()
  if (sha) params.set('sha', sha)
  if (author) params.set('author', author)
  if (since) params.set('since', since)
  if (until) params.set('until', until)
  params.set('per_page', String(perPage))
  params.set('page', String(page))
  const text = await apiFetch(`/api/github/repos/${owner}/${repo}/commits?${params}`)
  return parseJson(text)
}

// GET /api/github/repos/{owner}/{repo}/commits/{ref}
export async function getCommit({ owner, repo, ref, perPage = 30, page = 1 } = {}) {
  const params = new URLSearchParams()
  params.set('per_page', String(perPage))
  params.set('page', String(page))
  const text = await apiFetch(
    `/api/github/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/commits/${encodeURIComponent(ref)}?${params}`
  )
  return parseJson(text)
}
