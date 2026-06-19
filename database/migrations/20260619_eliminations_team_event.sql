-- eliminations: submission_id -> team_id + event_id (team/event scoped drops & eliminations).
-- Safe to re-run only on DBs that still have submission_id; fresh installs use schema.sql.

ALTER TABLE `eliminations`
  DROP FOREIGN KEY `fk_el_submission`;

ALTER TABLE `eliminations`
  DROP INDEX `idx_el_submission`,
  DROP COLUMN `submission_id`,
  ADD COLUMN `team_id` varchar(36) NOT NULL AFTER `elimination_id`,
  ADD COLUMN `event_id` varchar(36) NOT NULL AFTER `team_id`,
  ADD KEY `idx_el_team` (`team_id`),
  ADD KEY `idx_el_event` (`event_id`),
  ADD CONSTRAINT `fk_el_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  ADD CONSTRAINT `fk_el_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`);
