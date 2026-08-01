import { describe, expect, it } from 'vitest'
import { parseGitHubRepoUrl } from './githubRepo'

describe('parseGitHubRepoUrl', () => {
  it('parses standard github.com URLs', () => {
    expect(parseGitHubRepoUrl('https://github.com/org/repo')).toEqual({
      owner: 'org',
      repo: 'repo'
    })
  })

  it('strips trailing .git and slash', () => {
    expect(parseGitHubRepoUrl('https://github.com/org/my-repo.git/')).toEqual({
      owner: 'org',
      repo: 'my-repo'
    })
  })

  it('returns null for invalid URLs', () => {
    expect(parseGitHubRepoUrl(null)).toBeNull()
    expect(parseGitHubRepoUrl('https://gitlab.com/org/repo')).toBeNull()
    expect(parseGitHubRepoUrl('not-a-url')).toBeNull()
  })
})
