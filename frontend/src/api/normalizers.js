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

function mapGroupRow(row) {
  const maxRaw = row.maxTeams ?? row.max_teams
  const maxNum = maxRaw == null || maxRaw === '' ? null : Number(maxRaw)
  const teamCountRaw = row.teamCount ?? row.team_count
  const teamCountNum = teamCountRaw == null || teamCountRaw === '' ? 0 : Number(teamCountRaw)
  return {
    groupId: normalizeId(row.groupId ?? row.group_id),
    roundId: normalizeId(row.roundId ?? row.round_id),
    roundName: row.roundName ?? row.round_name ?? '',
    roundOrder: String(row.roundOrder ?? row.round_order ?? ''),
    name: row.name ?? '',
    maxTeams: Number.isFinite(maxNum) ? maxNum : null,
    teamCount: Number.isFinite(teamCountNum) ? teamCountNum : 0
  }
}

function mapRoundRow(row) {
  const winnersPerRoundRaw = row.winnersPerRound ?? row.winners_per_round
  const winnersPerRoundNum = winnersPerRoundRaw == null || winnersPerRoundRaw === '' ? 1 : Number(winnersPerRoundRaw)
  const winnerCountRaw = row.winnerCount ?? row.winner_count
  const winnerCountNum = winnerCountRaw == null || winnerCountRaw === '' ? 0 : Number(winnerCountRaw)
  return {
    roundId: normalizeId(row.roundId ?? row.round_id),
    name: row.name ?? '',
    roundOrder: String(row.roundOrder ?? row.round_order ?? ''),
    startDate: row.startDate ?? row.start_date ?? '',
    endDate: row.endDate ?? row.end_date ?? '',
    submissionDeadline: row.submissionDeadline ?? row.submission_deadline ?? '',
    winnersPerRound: Number.isFinite(winnersPerRoundNum) ? winnersPerRoundNum : 1,
    winnerCount: Number.isFinite(winnerCountNum) ? winnerCountNum : 0
  }
}

function mapOptionalInt(value) {
  if (value == null || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function mapTeamRow(row) {
  return {
    registrationId: normalizeId(row.registrationId ?? row.registration_id),
    teamId: normalizeId(row.teamId ?? row.team_id),
    teamName: row.teamName ?? row.team_name ?? '',
    status: row.status ?? '',
    githubStatus: row.githubStatus ?? row.github_status ?? '',
    githubRepoUrl: row.githubRepoUrl ?? row.github_repo_url ?? '',
    githubTeamSlug: row.githubTeamSlug ?? row.github_team_slug ?? ''
  }
}

function mapAssignedMentorRow(row) {
  const m = row && typeof row === 'object' ? row : {}
  return {
    roundId: normalizeId(m.roundId ?? m.round_id),
    roundName: m.roundName ?? m.round_name ?? '',
    groupId: normalizeId(m.groupId ?? m.group_id),
    groupName: m.groupName ?? m.group_name ?? '',
    mentorId: normalizeId(m.mentorId ?? m.mentor_id),
    mentorName: m.mentorName ?? m.mentor_name ?? '',
    mentorEmail: m.mentorEmail ?? m.mentor_email ?? ''
  }
}

function mapAssignedJudgeRow(row) {
  const j = row && typeof row === 'object' ? row : {}
  return {
    roundId: normalizeId(j.roundId ?? j.round_id),
    roundName: j.roundName ?? j.round_name ?? '',
    groupId: normalizeId(j.groupId ?? j.group_id),
    groupName: j.groupName ?? j.group_name ?? '',
    judgeId: normalizeId(j.judgeId ?? j.judge_id),
    judgeName: j.judgeName ?? j.judge_name ?? '',
    judgeEmail: j.judgeEmail ?? j.judge_email ?? ''
  }
}

function mapAwardRow(row) {
  const rankRaw = row.rank
  const rankNum = rankRaw == null || rankRaw === '' ? null : Number(rankRaw)
  return {
    awardId: normalizeId(row.awardId ?? row.award_id),
    title: row.title ?? '',
    rank: Number.isFinite(rankNum) ? rankNum : null,
    teamName: row.teamName ?? row.team_name ?? ''
  }
}

export function mapMentorAssignmentRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    eventId: normalizeId(r.eventId ?? r.event_id),
    eventTitle: r.eventTitle ?? r.event_title ?? '',
    roundId: normalizeId(r.roundId ?? r.round_id),
    roundName: r.roundName ?? r.round_name ?? '',
    groupId: normalizeId(r.groupId ?? r.group_id),
    groupName: r.groupName ?? r.group_name ?? ''
  }
}

function mapMentorAssignedTeamMemberRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    userId: normalizeId(r.userId ?? r.user_id),
    fullName: r.fullName ?? r.full_name ?? '',
    email: r.email ?? '',
    userRole: r.userRole ?? r.user_role ?? '',
    teamRole: r.teamRole ?? r.team_role ?? ''
  }
}

export function mapMentorAssignedTeamRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    eventId: normalizeId(r.eventId ?? r.event_id),
    eventTitle: r.eventTitle ?? r.event_title ?? '',
    roundId: normalizeId(r.roundId ?? r.round_id),
    roundName: r.roundName ?? r.round_name ?? '',
    groupId: normalizeId(r.groupId ?? r.group_id),
    groupName: r.groupName ?? r.group_name ?? '',
    registrationId: normalizeId(r.registrationId ?? r.registration_id),
    registrationStatus: r.registrationStatus ?? r.registration_status ?? '',
    registeredAt: r.registeredAt ?? r.registered_at ?? '',
    teamId: normalizeId(r.teamId ?? r.team_id),
    teamName: r.teamName ?? r.team_name ?? '',
    teamStatus: r.teamStatus ?? r.team_status ?? '',
    enrollCode: r.enrollCode ?? r.enroll_code ?? '',
    leaderId: normalizeId(r.leaderId ?? r.leader_id),
    leaderName: r.leaderName ?? r.leader_name ?? '',
    leaderEmail: r.leaderEmail ?? r.leader_email ?? '',
    memberCount: mapCount(r.memberCount ?? r.member_count),
    members: mapList(r.members, mapMentorAssignedTeamMemberRow)
  }
}

export function mapGroupColleagueRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    userId: normalizeId(r.userId ?? r.user_id),
    fullName: r.fullName ?? r.full_name ?? '',
    email: r.email ?? '',
    role: r.role ?? '',
    self: Boolean(r.self)
  }
}

export function mapGroupColleaguesResponse(data) {
  const r = data && typeof data === 'object' ? data : {}
  return {
    eventId: normalizeEventId(r.eventId ?? r.event_id),
    roundId: normalizeId(r.roundId ?? r.round_id),
    groupId: normalizeId(r.groupId ?? r.group_id),
    roundName: r.roundName ?? r.round_name ?? '',
    groupName: r.groupName ?? r.group_name ?? '',
    mentors: mapList(r.mentors, mapGroupColleagueRow),
    judges: mapList(r.judges, mapGroupColleagueRow)
  }
}

export function mapCriteriaResponse(data) {
  const r = data && typeof data === 'object' ? data : {}
  const criteria = Array.isArray(r.criteria) ? r.criteria : []
  const totalWeight = r.totalWeight ?? criteria.reduce((sum, c) => sum + (Number(c.weight) || 0), 0)
  const totalMaxScore = criteria.reduce((sum, c) => sum + (Number(c.maxScore) || 0), 0)
  return {
    roundId: normalizeId(r.roundId ?? r.round_id),
    criteria,
    count: criteria.length,
    totalWeight,
    totalMaxScore
  }
}

function mapJudgeScoreDetailRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    detailId: normalizeId(r.detailId ?? r.detail_id),
    scoreId: normalizeId(r.scoreId ?? r.score_id),
    criteriaId: normalizeId(r.criteriaId ?? r.criteria_id),
    score: r.score ?? null,
    feedback: r.feedback ?? ''
  }
}

export function mapJudgeScoreRow(data) {
  const r = data && typeof data === 'object' ? data : {}
  return {
    scoreId: normalizeId(r.scoreId ?? r.score_id),
    submissionId: normalizeId(r.submissionId ?? r.submission_id),
    judgeId: normalizeId(r.judgeId ?? r.judge_id),
    groupId: normalizeId(r.groupId ?? r.group_id),
    totalScore: r.totalScore ?? r.total_score ?? null,
    submittedAt: r.submittedAt ?? r.submitted_at ?? '',
    details: mapList(r.details, mapJudgeScoreDetailRow)
  }
}

export function mapTeamToScoreRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    teamId: normalizeId(r.teamId ?? r.team_id),
    teamName: r.teamName ?? r.team_name ?? '',
    submissionId: normalizeId(r.submissionId ?? r.submission_id),
    submissionStatus: r.submissionStatus ?? r.submission_status ?? '',
    submittedAt: r.submittedAt ?? r.submitted_at ?? '',
    githubUrl: r.githubUrl ?? r.github_url ?? '',
    demoUrl: r.demoUrl ?? r.demo_url ?? '',
    reportUrl: r.reportUrl ?? r.report_url ?? '',
    slideUrl: r.slideUrl ?? r.slide_url ?? '',
    scored: Boolean(r.scored),
    totalScore: r.totalScore ?? r.total_score ?? null,
    scoreId: normalizeId(r.scoreId ?? r.score_id)
  }
}

export function mapEventDetailRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    ...mapEventRow(r),
    maxTeams: mapOptionalInt(r.maxTeams ?? r.max_teams),
    numRounds: mapOptionalInt(r.numRounds ?? r.num_rounds) ?? 1,
    totalTeams: mapCount(r.totalTeams ?? r.total_teams),
    totalGroups: mapCount(r.totalGroups ?? r.total_groups),
    totalRounds: mapCount(r.totalRounds ?? r.total_rounds),
    totalAwards: mapCount(r.totalAwards ?? r.total_awards),
    githubTemplateRepo: r.githubTemplateRepo ?? r.github_template_repo ?? '',
    teams: mapList(r.teams, mapTeamRow),
    groups: mapList(r.groups, mapGroupRow),
    rounds: mapList(r.rounds, mapRoundRow),
    awards: mapList(r.awards, mapAwardRow),
    assignedMentors: mapList(r.assignedMentors ?? r.assigned_mentors, mapAssignedMentorRow),
    assignedJudges: mapList(r.assignedJudges ?? r.assigned_judges, mapAssignedJudgeRow)
  }
}
