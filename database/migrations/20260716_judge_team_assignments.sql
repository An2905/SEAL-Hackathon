-- Each team submission is assigned to one judge for a round.  Assignments are
-- created automatically once the round has ended.
CREATE TABLE judge_team_assignments (
  assignment_id varchar(36) NOT NULL,
  judge_id varchar(36) NOT NULL,
  round_id varchar(36) NOT NULL,
  group_id varchar(36) NOT NULL,
  team_id varchar(36) NOT NULL,
  assigned_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (assignment_id),
  UNIQUE KEY uq_jta_round_team (round_id, team_id),
  UNIQUE KEY uq_jta_judge_round_team (judge_id, round_id, team_id),
  KEY idx_jta_judge (judge_id),
  KEY idx_jta_group (group_id),
  CONSTRAINT fk_jta_judge FOREIGN KEY (judge_id) REFERENCES users (user_id),
  CONSTRAINT fk_jta_round FOREIGN KEY (round_id) REFERENCES rounds (round_id),
  CONSTRAINT fk_jta_group FOREIGN KEY (group_id) REFERENCES round_groups (group_id),
  CONSTRAINT fk_jta_team FOREIGN KEY (team_id) REFERENCES teams (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
