-- Corrective migration: Event/team queries map this GitHub team slug.
-- Run once on databases created before the column existed.
ALTER TABLE `team_registrations`
  ADD COLUMN `github_team_slug` VARCHAR(100) NULL DEFAULT NULL AFTER `github_status`;
