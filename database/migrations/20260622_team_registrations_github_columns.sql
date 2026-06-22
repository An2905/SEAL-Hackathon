-- Thêm các cột liên quan đến GitHub cho bảng team_registrations.
ALTER TABLE `team_registrations`
  ADD COLUMN `github_team_id` BIGINT NULL DEFAULT NULL AFTER `registered_at`,
  ADD COLUMN `github_team_slug` VARCHAR(100) NULL DEFAULT NULL AFTER `github_team_id`,
  ADD COLUMN `github_repo_id` BIGINT NULL DEFAULT NULL AFTER `github_team_slug`,
  ADD COLUMN `github_repo_url` VARCHAR(255) NULL DEFAULT NULL AFTER `github_repo_id`;
