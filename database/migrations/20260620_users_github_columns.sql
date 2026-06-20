-- Di chuyển github_username và github_id từ studentprofile sang users.

ALTER TABLE `users`
  ADD COLUMN `github_username` VARCHAR(100) NULL DEFAULT NULL AFTER `status`,
  ADD COLUMN `github_id` BIGINT NULL DEFAULT NULL AFTER `github_username`;

UPDATE `users` u
INNER JOIN `studentprofile` sp ON u.user_id = sp.user_id
SET u.github_username = sp.github_username,
    u.github_id = sp.github_id
WHERE sp.github_id IS NOT NULL
   OR (sp.github_username IS NOT NULL AND sp.github_username <> '');

ALTER TABLE `studentprofile`
  DROP COLUMN `github_username`,
  DROP COLUMN `github_id`;
