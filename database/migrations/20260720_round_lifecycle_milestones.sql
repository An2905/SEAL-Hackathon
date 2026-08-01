-- Tracks automated round lifecycle actions (start, submission close, end).
CREATE TABLE IF NOT EXISTS round_lifecycle_milestones (
  round_id varchar(36) NOT NULL,
  milestone varchar(50) NOT NULL,
  processed_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (round_id, milestone),
  CONSTRAINT fk_rlm_round FOREIGN KEY (round_id) REFERENCES rounds (round_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
