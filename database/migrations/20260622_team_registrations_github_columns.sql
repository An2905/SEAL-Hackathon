-- Thêm các cột liên quan đến GitHub cho bảng team_registrations.
ALTER TABLE `team_registrations`
  ADD COLUMN `github_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER `registered_at`,
  ADD COLUMN `github_team_id` BIGINT NULL DEFAULT NULL AFTER `github_status`,
  ADD COLUMN `github_team_slug` VARCHAR(100) NULL DEFAULT NULL AFTER `github_team_id`,
  ADD COLUMN `github_repo_id` BIGINT NULL DEFAULT NULL AFTER `github_team_slug`,
  ADD COLUMN `github_repo_url` VARCHAR(255) NULL DEFAULT NULL AFTER `github_repo_id`;

-- Thêm cột cấu hình repository template cho bảng events.
ALTER TABLE `events`
  ADD COLUMN `github_template_repo` VARCHAR(100) NULL DEFAULT NULL AFTER `num_rounds`;
