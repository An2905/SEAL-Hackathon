CREATE TABLE IF NOT EXISTS github_repository_access_tasks (
  task_id VARCHAR(36) NOT NULL,
  round_id VARCHAR(36) NOT NULL,
  group_id VARCHAR(36) NULL,
  team_id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  github_repo_url VARCHAR(255) NOT NULL,
  operation VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempt_count INT NOT NULL DEFAULT 0,
  last_error TEXT NULL,
  next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (task_id),

  UNIQUE KEY uq_github_access_task (
    round_id,
    team_id,
    user_id,
    operation
  ),

  KEY idx_github_access_task_retry (
    status,
    next_retry_at
  ),

  KEY idx_github_access_task_round (
    round_id
  ),

  CONSTRAINT fk_github_access_task_round
    FOREIGN KEY (round_id) REFERENCES rounds(round_id),

  CONSTRAINT fk_github_access_task_group
    FOREIGN KEY (group_id) REFERENCES round_groups(group_id),

  CONSTRAINT fk_github_access_task_team
    FOREIGN KEY (team_id) REFERENCES teams(team_id),

  CONSTRAINT fk_github_access_task_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
