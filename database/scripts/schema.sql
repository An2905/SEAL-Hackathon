-- SEAL Hackathon — MySQL schema v4 (file schema duy nhất — copy/paste chạy một lần)
-- Mô hình: Event → Round → Group → Team
-- Event status: BUILDING → UPCOMING → ONGOING → COMPLETED
-- Role expert: EXPERT_INTERNAL, EXPERT_EXTERNAL (mentor/judge qua mentor_assignments / judge_assignments)
-- Bảng hồ sơ expert: participants_profile (phone, avatar_url, participant_type)
-- ID: VARCHAR(36) UUID — app/backend tự sinh, không AUTO_INCREMENT
-- Chat: polling (lastMessageId) — không WebSocket
-- Seed (tùy chọn, chạy sau): hackathon_mysql_v4_seed.sql

CREATE DATABASE IF NOT EXISTS `hackathon`;
USE `hackathon`;

SET FOREIGN_KEY_CHECKS = 0;

-- Legacy / bảng cũ (nếu còn)
DROP TABLE IF EXISTS `advancement_rules`;
DROP TABLE IF EXISTS `category_mentors`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `calibration_scores`;
DROP TABLE IF EXISTS `calibration_rounds`;

-- v4
DROP TABLE IF EXISTS `chat_messages`;
DROP TABLE IF EXISTS `chat_room_members`;
DROP TABLE IF EXISTS `chat_rooms`;
DROP TABLE IF EXISTS `score_details`;
DROP TABLE IF EXISTS `scores`;
DROP TABLE IF EXISTS `group_winners`;
DROP TABLE IF EXISTS `round_winners`;
DROP TABLE IF EXISTS `eliminations`;
DROP TABLE IF EXISTS `submissions`;
DROP TABLE IF EXISTS `judge_assignments`;
DROP TABLE IF EXISTS `mentor_assignments`;
DROP TABLE IF EXISTS `check_ins`;
DROP TABLE IF EXISTS `group_teams`;
DROP TABLE IF EXISTS `team_registrations`;
DROP TABLE IF EXISTS `team_members`;
DROP TABLE IF EXISTS `event_criteria`;
DROP TABLE IF EXISTS `announcements`;
DROP TABLE IF EXISTS `awards`;
DROP TABLE IF EXISTS `audit_logs`;
DROP TABLE IF EXISTS `criteria_template_items`;
DROP TABLE IF EXISTS `criteria_templates`;
DROP TABLE IF EXISTS `round_groups`;
DROP TABLE IF EXISTS `rounds`;
DROP TABLE IF EXISTS `teams`;
DROP TABLE IF EXISTS `events`;
DROP TABLE IF EXISTS `studentprofile`;
DROP TABLE IF EXISTS `participants_profile`;
DROP TABLE IF EXISTS `judge_profiles`;
DROP TABLE IF EXISTS `universities`;
DROP TABLE IF EXISTS `users`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE `users` (
  `user_id`       VARCHAR(36) NOT NULL,
  `full_name`     VARCHAR(100) NULL,
  `email`         VARCHAR(150) NULL,
  `password_hash` LONGTEXT NULL,
  `role`          VARCHAR(30) NOT NULL,
  `status`        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `chk_users_role` CHECK (`role` IN (
    'COORDINATOR',
    'EXPERT_INTERNAL',
    'EXPERT_EXTERNAL',
    'STUDENT_FPT',
    'STUDENT_EXTERNAL'
  )),
  CONSTRAINT `chk_users_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- ============================================================
-- 2. PARTICIPANTS_PROFILE (mentor / judge)
-- ============================================================
CREATE TABLE `participants_profile` (
  `profile_id`       VARCHAR(36) NOT NULL,
  `user_id`          VARCHAR(36) NOT NULL,
  `phone`            VARCHAR(20) NULL,
  `avatar_url`       VARCHAR(512) NULL,
  `organization`     VARCHAR(200) NULL,
  `bio`              LONGTEXT NULL,
  `participant_type` VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
  `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uq_pp_user` (`user_id`),
  CONSTRAINT `chk_pp_type` CHECK (`participant_type` IN ('INTERNAL', 'EXTERNAL'))
);

-- ============================================================
-- 3. STUDENT_PROFILES
-- ============================================================
CREATE TABLE `studentprofile` (
  `profile_id`      VARCHAR(36) NOT NULL,
  `user_id`         VARCHAR(36) NOT NULL,
  `student_code`    VARCHAR(30) NULL,
  `university_name` VARCHAR(150) NULL,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uq_sp_user` (`user_id`)
);

CREATE TABLE `universities` (
  `university_id`   VARCHAR(36) NOT NULL,
  `university_name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`university_id`),
  UNIQUE KEY `uq_uni_name` (`university_name`)
);

-- ============================================================
-- 4. EVENTS
-- ============================================================
CREATE TABLE `events` (
  `event_id`    VARCHAR(36) NOT NULL,
  `title`       VARCHAR(200) NOT NULL,
  `description` LONGTEXT NULL,
  `start_date`  DATETIME NULL,
  `end_date`    DATETIME NULL,
  `status`      VARCHAR(20) NOT NULL DEFAULT 'BUILDING',
  `max_teams`   INT NULL,
  `num_rounds`  INT NOT NULL DEFAULT 1,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  CONSTRAINT `chk_events_status` CHECK (`status` IN ('BUILDING', 'UPCOMING', 'ONGOING', 'COMPLETED'))
);

-- ============================================================
-- 5. ROUNDS
-- ============================================================
CREATE TABLE `rounds` (
  `round_id`            VARCHAR(36) NOT NULL,
  `event_id`            VARCHAR(36) NOT NULL,
  `name`                VARCHAR(100) NOT NULL,
  `round_order`         INT NOT NULL,
  `num_groups`          INT NOT NULL DEFAULT 1,
  `max_teams_per_group` INT NULL,
  `winners_per_round`   INT NOT NULL DEFAULT 1,
  `submission_deadline` DATETIME NULL,
  `start_date`          DATETIME NULL,
  `end_date`            DATETIME NULL,
  `created_at`          DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_id`),
  KEY `idx_rounds_event` (`event_id`)
);

-- ============================================================
-- 6. ROUND_GROUPS
-- ============================================================
CREATE TABLE `round_groups` (
  `group_id`   VARCHAR(36) NOT NULL,
  `round_id`   VARCHAR(36) NOT NULL,
  `name`       VARCHAR(100) NOT NULL,
  `max_teams`  INT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`),
  KEY `idx_rg_round` (`round_id`)
);

-- ============================================================
-- 7. TEAMS
-- ============================================================
CREATE TABLE `teams` (
  `team_id`     VARCHAR(36) NOT NULL,
  `team_name`   VARCHAR(100) NOT NULL,
  `leader_id`   VARCHAR(36) NOT NULL,
  `status`      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `max_members` INT NOT NULL DEFAULT 5,
  `enrollCode`  VARCHAR(50) NOT NULL DEFAULT 'TEMP_CODE',
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `uq_team_name` (`team_name`),
  KEY `idx_teams_leader` (`leader_id`),
  CONSTRAINT `chk_teams_status` CHECK (`status` IN ('ACTIVE', 'ELIMINATED', 'DISQUALIFIED', 'WITHDRAWN'))
);

-- ============================================================
-- 8. TEAM_MEMBERS
-- ============================================================
CREATE TABLE `team_members` (
  `team_id`   VARCHAR(36) NOT NULL,
  `user_id`   VARCHAR(36) NOT NULL,
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`, `user_id`),
  KEY `idx_tm_user` (`user_id`)
);

-- ============================================================
-- 9. TEAM_REGISTRATIONS (đăng ký event — không chọn category/track;
--    phân bảng sau qua group_teams khi BTC gán đội vào round_groups)
-- ============================================================
CREATE TABLE `team_registrations` (
  `registration_id` VARCHAR(36) NOT NULL,
  `event_id`        VARCHAR(36) NOT NULL,
  `team_id`         VARCHAR(36) NOT NULL,
  `status`          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `registered_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`registration_id`),
  UNIQUE KEY `uq_tr_event_team` (`event_id`, `team_id`),
  KEY `idx_tr_team` (`team_id`),
  CONSTRAINT `chk_tr_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
);

-- ============================================================
-- 10. GROUP_TEAMS
-- ============================================================
CREATE TABLE `group_teams` (
  `group_id`    VARCHAR(36) NOT NULL,
  `round_id`    VARCHAR(36) NOT NULL,
  `team_id`     VARCHAR(36) NOT NULL,
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`, `team_id`),
  UNIQUE KEY `uq_gt_team_round` (`team_id`, `round_id`),
  KEY `idx_gt_team` (`team_id`),
  KEY `idx_gt_round` (`round_id`)
);

-- ============================================================
-- 11. CHECK_INS
-- ============================================================
CREATE TABLE `check_ins` (
  `checkin_id` VARCHAR(36) NOT NULL,
  `event_id`   VARCHAR(36) NOT NULL,
  `team_id`    VARCHAR(36) NOT NULL,
  `user_id`    VARCHAR(36) NOT NULL,
  `checked_by` VARCHAR(36) NOT NULL,
  `checked_in` TINYINT(1) NOT NULL DEFAULT 0,
  `checked_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uq_checkin` (`event_id`, `team_id`, `user_id`),
  KEY `idx_ci_team` (`team_id`)
);

-- ============================================================
-- 12. MENTOR_ASSIGNMENTS
-- ============================================================
CREATE TABLE `mentor_assignments` (
  `assignment_id` VARCHAR(36) NOT NULL,
  `mentor_id`     VARCHAR(36) NOT NULL,
  `round_id`      VARCHAR(36) NOT NULL,
  `group_id`      VARCHAR(36) NOT NULL,
  `assigned_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uq_ma_mentor_round_group` (`mentor_id`, `round_id`, `group_id`),
  KEY `idx_ma_round` (`round_id`),
  KEY `idx_ma_group` (`group_id`)
);

-- ============================================================
-- 13. JUDGE_ASSIGNMENTS
-- ============================================================
CREATE TABLE `judge_assignments` (
  `assignment_id` VARCHAR(36) NOT NULL,
  `judge_id`      VARCHAR(36) NOT NULL,
  `round_id`      VARCHAR(36) NOT NULL,
  `group_id`      VARCHAR(36) NOT NULL,
  `assigned_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uq_ja_judge_round_group` (`judge_id`, `round_id`, `group_id`),
  KEY `idx_ja_round` (`round_id`),
  KEY `idx_ja_group` (`group_id`)
);

-- ============================================================
-- 14. SUBMISSIONS
-- ============================================================
CREATE TABLE `submissions` (
  `submission_id`       VARCHAR(36) NOT NULL,
  `team_id`             VARCHAR(36) NOT NULL,
  `round_id`            VARCHAR(36) NOT NULL,
  `group_id`            VARCHAR(36) NOT NULL,
  `github_url`          LONGTEXT NULL,
  `demo_url`            LONGTEXT NULL,
  `report_url`          LONGTEXT NULL,
  `slide_url`           LONGTEXT NULL,
  `repository_metadata` LONGTEXT NULL,
  `status`              VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  `submitted_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`submission_id`),
  UNIQUE KEY `uq_sub_team_round` (`team_id`, `round_id`),
  KEY `idx_sub_round` (`round_id`),
  KEY `idx_sub_group` (`group_id`),
  CONSTRAINT `chk_sub_status` CHECK (`status` IN ('SUBMITTED', 'LATE', 'DISQUALIFIED'))
);

-- ============================================================
-- 15. SCORING
-- ============================================================
CREATE TABLE `event_criteria` (
  `criteria_id`    VARCHAR(36) NOT NULL,
  `event_id`       VARCHAR(36) NOT NULL,
  `criterion_name` VARCHAR(100) NOT NULL,
  `weight`         DECIMAL(5,2) NOT NULL,
  `max_score`      DECIMAL(5,2) NOT NULL,
  `description`    LONGTEXT NULL,
  `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`criteria_id`),
  KEY `idx_ec_event` (`event_id`)
);

CREATE TABLE `scores` (
  `score_id`      VARCHAR(36) NOT NULL,
  `submission_id` VARCHAR(36) NOT NULL,
  `judge_id`      VARCHAR(36) NOT NULL,
  `group_id`      VARCHAR(36) NOT NULL,
  `total_score`   DECIMAL(6,2) NULL,
  `submitted_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`score_id`),
  UNIQUE KEY `uq_sc_submission_judge` (`submission_id`, `judge_id`),
  KEY `idx_sc_judge` (`judge_id`),
  KEY `idx_sc_group` (`group_id`)
);

CREATE TABLE `score_details` (
  `detail_id`   VARCHAR(36) NOT NULL,
  `score_id`    VARCHAR(36) NOT NULL,
  `criteria_id` VARCHAR(36) NOT NULL,
  `score`       DECIMAL(5,2) NOT NULL,
  `feedback`    LONGTEXT NULL,
  PRIMARY KEY (`detail_id`),
  KEY `idx_sd_score` (`score_id`),
  KEY `idx_sd_criteria` (`criteria_id`)
);

-- ============================================================
-- 16. WINNERS
-- ============================================================
CREATE TABLE `group_winners` (
  `winner_id`   VARCHAR(36) NOT NULL,
  `group_id`    VARCHAR(36) NOT NULL,
  `team_id`     VARCHAR(36) NOT NULL,
  `rank`        INT NOT NULL,
  `total_score` DECIMAL(6,2) NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`winner_id`),
  UNIQUE KEY `uq_group_winner` (`group_id`, `team_id`),
  KEY `idx_gw_team` (`team_id`)
);

CREATE TABLE `round_winners` (
  `round_winner_id`      VARCHAR(36) NOT NULL,
  `round_id`             VARCHAR(36) NOT NULL,
  `team_id`              VARCHAR(36) NOT NULL,
  `rank`                 INT NULL,
  `total_score`          DECIMAL(6,2) NULL,
  `advanced_to_round_id` VARCHAR(36) NULL,
  `staff_confirmed`      TINYINT(1) NOT NULL DEFAULT 0,
  `confirmed_by`         VARCHAR(36) NULL,
  `created_at`           DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_winner_id`),
  UNIQUE KEY `uq_round_winner` (`round_id`, `team_id`),
  KEY `idx_rw_team` (`team_id`)
);

-- ============================================================
-- 17. CHAT
-- ============================================================
CREATE TABLE `chat_rooms` (
  `room_id`    VARCHAR(36) NOT NULL,
  `event_id`   VARCHAR(36) NOT NULL,
  `round_id`   VARCHAR(36) NOT NULL,
  `group_id`   VARCHAR(36) NOT NULL,
  `team_id`    VARCHAR(36) NOT NULL,
  `mentor_id`  VARCHAR(36) NOT NULL,
  `created_by` VARCHAR(36) NOT NULL,
  `status`     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `closed_at`  DATETIME NULL,
  PRIMARY KEY (`room_id`),
  UNIQUE KEY `uq_chat_room` (`team_id`, `mentor_id`, `round_id`),
  KEY `idx_cr_event` (`event_id`),
  KEY `idx_cr_group` (`group_id`),
  KEY `idx_cr_mentor` (`mentor_id`),
  CONSTRAINT `chk_cr_status` CHECK (`status` IN ('ACTIVE', 'CLOSED'))
);

CREATE TABLE `chat_room_members` (
  `room_member_id` VARCHAR(36) NOT NULL,
  `room_id`        VARCHAR(36) NOT NULL,
  `user_id`        VARCHAR(36) NOT NULL,
  `joined_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`room_member_id`),
  UNIQUE KEY `uq_crm_room_user` (`room_id`, `user_id`),
  KEY `idx_crm_user` (`user_id`)
);

CREATE TABLE `chat_messages` (
  `message_id` VARCHAR(36) NOT NULL,
  `room_id`    VARCHAR(36) NOT NULL,
  `sender_id`  VARCHAR(36) NOT NULL,
  `content`    TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_cm_room` (`room_id`),
  KEY `idx_cm_room_created` (`room_id`, `created_at`)
);

-- ============================================================
-- 18. KHÁC
-- ============================================================
CREATE TABLE `announcements` (
  `announcement_id` VARCHAR(36) NOT NULL,
  `event_id`        VARCHAR(36) NOT NULL,
  `title`           VARCHAR(200) NOT NULL,
  `content`         LONGTEXT NOT NULL,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`announcement_id`),
  KEY `idx_ann_event` (`event_id`)
);

CREATE TABLE `awards` (
  `award_id`   VARCHAR(36) NOT NULL,
  `event_id`   VARCHAR(36) NOT NULL,
  `team_id`    VARCHAR(36) NULL,
  `title`      VARCHAR(100) NOT NULL,
  `rank`       INT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`award_id`),
  KEY `idx_aw_event` (`event_id`)
);

CREATE TABLE `audit_logs` (
  `log_id`      VARCHAR(36) NOT NULL,
  `user_id`     VARCHAR(36) NOT NULL,
  `action`      VARCHAR(100) NOT NULL,
  `entity_type` VARCHAR(100) NULL,
  `entity_id`   VARCHAR(36) NULL,
  `description` LONGTEXT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_al_user` (`user_id`)
);

CREATE TABLE `eliminations` (
  `elimination_id` VARCHAR(36) NOT NULL,
  `submission_id`  VARCHAR(36) NOT NULL,
  `reason`         LONGTEXT NOT NULL,
  `eliminated_by`  VARCHAR(36) NOT NULL,
  `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`elimination_id`),
  KEY `idx_el_submission` (`submission_id`)
);

CREATE TABLE `criteria_templates` (
  `template_id` VARCHAR(36) NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` LONGTEXT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`)
);

CREATE TABLE `criteria_template_items` (
  `item_id`        VARCHAR(36) NOT NULL,
  `template_id`    VARCHAR(36) NOT NULL,
  `criterion_name` VARCHAR(100) NOT NULL,
  `weight`         DECIMAL(5,2) NOT NULL,
  `max_score`      DECIMAL(5,2) NOT NULL,
  `description`    LONGTEXT NULL,
  PRIMARY KEY (`item_id`),
  KEY `idx_cti_template` (`template_id`)
);

-- ============================================================
-- FOREIGN KEYS
-- ============================================================
ALTER TABLE `participants_profile`    ADD CONSTRAINT `fk_pp_user`         FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `studentprofile`          ADD CONSTRAINT `fk_sp_user`         FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `rounds`                  ADD CONSTRAINT `fk_rounds_event`    FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `round_groups`            ADD CONSTRAINT `fk_rg_round`        FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `teams`                   ADD CONSTRAINT `fk_teams_leader`    FOREIGN KEY (`leader_id`)             REFERENCES `users`(`user_id`);
ALTER TABLE `team_members`            ADD CONSTRAINT `fk_tm_team`         FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `team_members`            ADD CONSTRAINT `fk_tm_user`         FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `team_registrations`      ADD CONSTRAINT `fk_tr_event`         FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `team_registrations`      ADD CONSTRAINT `fk_tr_team`          FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `group_teams`             ADD CONSTRAINT `fk_gt_group`         FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `group_teams`             ADD CONSTRAINT `fk_gt_round`        FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `group_teams`             ADD CONSTRAINT `fk_gt_team`          FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `check_ins`               ADD CONSTRAINT `fk_ci_event`         FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `check_ins`               ADD CONSTRAINT `fk_ci_team`          FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `check_ins`               ADD CONSTRAINT `fk_ci_user`          FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `check_ins`               ADD CONSTRAINT `fk_ci_staff`         FOREIGN KEY (`checked_by`)            REFERENCES `users`(`user_id`);
ALTER TABLE `mentor_assignments`      ADD CONSTRAINT `fk_ma_mentor`        FOREIGN KEY (`mentor_id`)             REFERENCES `users`(`user_id`);
ALTER TABLE `mentor_assignments`      ADD CONSTRAINT `fk_ma_round`         FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `mentor_assignments`      ADD CONSTRAINT `fk_ma_group`         FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `judge_assignments`       ADD CONSTRAINT `fk_ja_judge`         FOREIGN KEY (`judge_id`)              REFERENCES `users`(`user_id`);
ALTER TABLE `judge_assignments`       ADD CONSTRAINT `fk_ja_round`         FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `judge_assignments`       ADD CONSTRAINT `fk_ja_group`         FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `submissions`             ADD CONSTRAINT `fk_sub_team`          FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `submissions`             ADD CONSTRAINT `fk_sub_round`         FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `submissions`             ADD CONSTRAINT `fk_sub_group`         FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `event_criteria`          ADD CONSTRAINT `fk_ec_event`         FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `scores`                  ADD CONSTRAINT `fk_sc_submission`    FOREIGN KEY (`submission_id`)         REFERENCES `submissions`(`submission_id`);
ALTER TABLE `scores`                  ADD CONSTRAINT `fk_sc_judge`          FOREIGN KEY (`judge_id`)              REFERENCES `users`(`user_id`);
ALTER TABLE `scores`                  ADD CONSTRAINT `fk_sc_group`          FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `score_details`           ADD CONSTRAINT `fk_sd_score`          FOREIGN KEY (`score_id`)              REFERENCES `scores`(`score_id`);
ALTER TABLE `score_details`           ADD CONSTRAINT `fk_sd_criteria`      FOREIGN KEY (`criteria_id`)           REFERENCES `event_criteria`(`criteria_id`);
ALTER TABLE `group_winners`           ADD CONSTRAINT `fk_gw_group`          FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `group_winners`           ADD CONSTRAINT `fk_gw_team`           FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `round_winners`           ADD CONSTRAINT `fk_rw_round`          FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `round_winners`           ADD CONSTRAINT `fk_rw_team`           FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `round_winners`           ADD CONSTRAINT `fk_rw_next_round`    FOREIGN KEY (`advanced_to_round_id`)  REFERENCES `rounds`(`round_id`);
ALTER TABLE `round_winners`           ADD CONSTRAINT `fk_rw_confirmed_by`  FOREIGN KEY (`confirmed_by`)          REFERENCES `users`(`user_id`);
ALTER TABLE `chat_rooms`              ADD CONSTRAINT `fk_cr_event`         FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `chat_rooms`              ADD CONSTRAINT `fk_cr_round`         FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `chat_rooms`              ADD CONSTRAINT `fk_cr_group`         FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `chat_rooms`              ADD CONSTRAINT `fk_cr_team`          FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `chat_rooms`              ADD CONSTRAINT `fk_cr_mentor`        FOREIGN KEY (`mentor_id`)             REFERENCES `users`(`user_id`);
ALTER TABLE `chat_rooms`              ADD CONSTRAINT `fk_cr_created_by`   FOREIGN KEY (`created_by`)            REFERENCES `users`(`user_id`);
ALTER TABLE `chat_room_members`       ADD CONSTRAINT `fk_crm_room`          FOREIGN KEY (`room_id`)               REFERENCES `chat_rooms`(`room_id`) ON DELETE CASCADE;
ALTER TABLE `chat_room_members`       ADD CONSTRAINT `fk_crm_user`          FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `chat_messages`           ADD CONSTRAINT `fk_cm_room`          FOREIGN KEY (`room_id`)               REFERENCES `chat_rooms`(`room_id`) ON DELETE CASCADE;
ALTER TABLE `chat_messages`           ADD CONSTRAINT `fk_cm_sender`        FOREIGN KEY (`sender_id`)             REFERENCES `users`(`user_id`);
ALTER TABLE `announcements`           ADD CONSTRAINT `fk_ann_event`        FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `awards`                  ADD CONSTRAINT `fk_aw_event`          FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `awards`                  ADD CONSTRAINT `fk_aw_team`           FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `audit_logs`              ADD CONSTRAINT `fk_al_user`           FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `eliminations`            ADD CONSTRAINT `fk_el_submission`    FOREIGN KEY (`submission_id`)         REFERENCES `submissions`(`submission_id`);
ALTER TABLE `eliminations`            ADD CONSTRAINT `fk_el_user`           FOREIGN KEY (`eliminated_by`)         REFERENCES `users`(`user_id`);
ALTER TABLE `criteria_template_items` ADD CONSTRAINT `fk_cti_template`     FOREIGN KEY (`template_id`)           REFERENCES `criteria_templates`(`template_id`);
