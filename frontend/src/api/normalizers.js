// Shared data normalizers & row mappers for the API layer.
//
// These live here (instead of inside each api/*.js) so the api modules stay
// thin like auth.js: every api function should only call apiFetch, check the
// response, and return. Parsing/shaping of BE payloads happens here.

// DB ids (event_id, user_id, registration_id...) are bigint and sometimes
// arrive as floats (e.g. "12.0"). Normalize to a plain integer string before
// sending them back to the BE.
export function normalizeId(value) {
  if (value == null || value === '') return ''
  const num = Number(value)
  if (Number.isFinite(num)) return String(Math.trunc(num))
  const s = String(value).trim()
  const dot = s.indexOf('.')
  if (dot > 0 && /^\d+\.0+$/.test(s)) return s.slice(0, dot)
  return s
}

// Domain-named aliases — same logic, kept descriptive at the call site.
export const normalizeEventId = normalizeId
export const normalizeAccountUserId = normalizeId
export const normalizeRegistrationId = normalizeId

// Count-like values: always return a clean integer string ("0" as fallback).
export function mapCount(value) {
  if (value == null || value === '') return '0'
  const num = Number(value)
  return Number.isFinite(num) ? String(Math.trunc(num)) : String(value)
}

export function mapList(value, mapper) {
  return Array.isArray(value) ? value.map(mapper) : []
}

export function countPendingTeams(teams) {
  if (!Array.isArray(teams)) return '0'
  const n = teams.filter(
    (t) =>
      String(t?.status ?? '')
        .trim()
        .toUpperCase() === 'PENDING'
  ).length
  return String(n)
}

export function mapAccountRow(row) {
  return {
    userId: normalizeId(row.userId ?? row.user_id),
    email: row.email ?? '',
    fullName: row.fullName ?? row.full_name ?? '',
    role: row.role ?? '',
    status: row.status ?? ''
  }
}

export function mapEventRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    eventId: normalizeId(r.eventId ?? r.event_id),
    title: r.title ?? '',
    description: r.description ?? '',
    startDate: r.startDate ?? r.start_date ?? '',
    endDate: r.endDate ?? r.end_date ?? '',
    status: r.status ?? '',
    createdAt: r.createdAt ?? r.created_at ?? ''
  }
}

function mapCategoryRow(row) {
  return {
    categoryId: normalizeId(row.categoryId ?? row.category_id),
    name: row.name ?? '',
    description: row.description ?? ''
  }
}

function mapRoundRow(row) {
  return {
    roundId: normalizeId(row.roundId ?? row.round_id),
    name: row.name ?? '',
    startDate: row.startDate ?? row.start_date ?? '',
    endDate: row.endDate ?? row.end_date ?? '',
    submissionDeadline: row.submissionDeadline ?? row.submission_deadline ?? ''
  }
}

function mapTeamRow(row) {
  return {
    registrationId: normalizeId(row.registrationId ?? row.registration_id),
    teamId: normalizeId(row.teamId ?? row.team_id),
    teamName: row.teamName ?? row.team_name ?? '',
    status: row.status ?? ''
  }
}

function mapAwardRow(row) {
  return {
    awardId: normalizeId(row.awardId ?? row.award_id),
    title: row.title ?? '',
    rank: row.rank ?? '',
    teamName: row.teamName ?? row.team_name ?? ''
  }
}

export function mapEventDetailRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    ...mapEventRow(r),
    totalTeams: mapCount(r.totalTeams ?? r.total_teams),
    totalCategories: mapCount(r.totalCategories ?? r.total_categories),
    totalRounds: mapCount(r.totalRounds ?? r.total_rounds),
    totalAwards: mapCount(r.totalAwards ?? r.total_awards),
    teams: mapList(r.teams, mapTeamRow),
    categories: mapList(r.categories, mapCategoryRow),
    rounds: mapList(r.rounds, mapRoundRow),
    awards: mapList(r.awards, mapAwardRow)
  }
}
