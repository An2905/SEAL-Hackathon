-- Bỏ profile_id (user_id làm PK) và xóa cột bio khỏi participants_profile.

ALTER TABLE `participants_profile`
  DROP PRIMARY KEY,
  DROP INDEX `uq_pp_user`,
  DROP COLUMN `profile_id`,
  DROP COLUMN `bio`,
  ADD PRIMARY KEY (`user_id`);

ALTER TABLE `studentprofile`
  DROP PRIMARY KEY,
  DROP INDEX `uq_sp_user`,
  DROP COLUMN `profile_id`,
  ADD PRIMARY KEY (`user_id`);
