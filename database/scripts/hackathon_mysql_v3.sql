CREATE DATABASE IF NOT EXISTS `Hackathon`;
USE `Hackathon`;

-- ============================================================
-- 1. USERS (sửa: mở rộng role thêm JUDGE_EXTERNAL)
-- ============================================================
CREATE TABLE `users` (
  `user_id`       BIGINT AUTO_INCREMENT NOT NULL,
  `full_name`     VARCHAR(100) NULL,
  `email`         VARCHAR(150) NULL,
  `password_hash` LONGTEXT NULL,
  `role`          VARCHAR(30) NOT NULL,
  `status`        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `chk_users_role` CHECK (`role` IN (
    'COORDINATOR',
    'MENTOR',
    'JUDGE_INTERNAL',
    'JUDGE_EXTERNAL',   -- MỚI: giám khảo bên ngoài
    'STUDENT_FPT',
    'STUDENT_EXTERNAL'
  )),
  CONSTRAINT `chk_users_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- ============================================================
-- 2. JUDGE_PROFILES (MỚI: profile riêng cho JUDGE + MENTOR)
--    Lưu phone, organization, type internal/external
-- ============================================================
CREATE TABLE `judge_profiles` (
  `profile_id`    BIGINT AUTO_INCREMENT NOT NULL,
  `user_id`       BIGINT NOT NULL,
  `phone`         VARCHAR(20) NULL,
  `organization`  VARCHAR(200) NULL     COMMENT 'Công ty / trường / tổ chức',
  `bio`           LONGTEXT NULL,
  `judge_type`    VARCHAR(20) NOT NULL DEFAULT 'INTERNAL'
                  COMMENT 'INTERNAL = nội bộ, EXTERNAL = mời ngoài',
  `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `uq_jp_user` (`user_id`),
  CONSTRAINT `chk_jp_type` CHECK (`judge_type` IN ('INTERNAL', 'EXTERNAL'))
);

-- ============================================================
-- 3. STUDENT_PROFILES (giữ nguyên, đổi tên cho nhất quán)
-- ============================================================
CREATE TABLE `studentProfile` (
  `profile_id`      BIGINT AUTO_INCREMENT NOT NULL,
  `user_id`         BIGINT NOT NULL,
  `student_code`    VARCHAR(30) NULL,
  `university_name` VARCHAR(150) NULL,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`)
);

CREATE TABLE `universities` (
  `university_id`   BIGINT AUTO_INCREMENT NOT NULL,
  `university_name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`university_id`),
  UNIQUE KEY `uq_uni_name` (`university_name`)
);

-- ============================================================
-- 4. EVENTS (sửa: thêm max_teams, num_rounds)
-- ============================================================
CREATE TABLE `events` (
  `event_id`    BIGINT AUTO_INCREMENT NOT NULL,
  `title`       VARCHAR(200) NOT NULL,
  `description` LONGTEXT NULL,
  `start_date`  DATETIME NULL,
  `end_date`    DATETIME NULL,
  `status`      VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
  `max_teams`   INT NULL     COMMENT 'Giới hạn tổng số team tham gia',
  `num_rounds`  INT NOT NULL DEFAULT 1 COMMENT 'Số vòng, nhập khi tạo event',
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  CONSTRAINT `chk_events_status` CHECK (`status` IN ('UPCOMING', 'ONGOING', 'COMPLETED'))
);

-- ============================================================
-- 5. CATEGORIES (giữ nguyên)
-- ============================================================
CREATE TABLE `categories` (
  `category_id` BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`    BIGINT NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` LONGTEXT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`)
);

-- ============================================================
-- 6. ROUNDS (sửa: thêm num_groups, winners_per_group)
-- ============================================================
CREATE TABLE `rounds` (
  `round_id`            BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`            BIGINT NOT NULL,
  `name`                VARCHAR(100) NOT NULL,
  `round_order`         INT NOT NULL,
  `num_groups`          INT NOT NULL DEFAULT 1  COMMENT 'Số bảng trong vòng này',
  `max_teams_per_group` INT NULL               COMMENT 'Số team tối đa mỗi bảng',
  `winners_per_group`   INT NOT NULL DEFAULT 1  COMMENT 'Số team advance từ mỗi bảng',
  `submission_deadline` DATETIME NULL,
  `start_date`          DATETIME NULL,
  `end_date`            DATETIME NULL,
  `created_at`          DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_id`)
);

-- ============================================================
-- 7. ROUND_GROUPS (MỚI: các bảng trong một vòng)
-- ============================================================
CREATE TABLE `round_groups` (
  `group_id`   BIGINT AUTO_INCREMENT NOT NULL,
  `round_id`   BIGINT NOT NULL,
  `name`       VARCHAR(100) NOT NULL COMMENT 'Ví dụ: Bảng A, Bảng B',
  `max_teams`  INT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`)
);

-- ============================================================
-- 8. TEAMS (sửa: thêm max_members)
-- ============================================================
CREATE TABLE `teams` (
  `team_id`     BIGINT AUTO_INCREMENT NOT NULL,
  `team_name`   VARCHAR(100) NOT NULL,
  `leader_id`   BIGINT NOT NULL,
  `status`      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `max_members` INT NOT NULL DEFAULT 5 COMMENT 'Số thành viên tối đa',
  `enrollCode`  VARCHAR(50) NOT NULL DEFAULT 'TEMP_CODE',
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `uq_team_name` (`team_name`),
  CONSTRAINT `chk_teams_status` CHECK (`status` IN ('ACTIVE', 'ELIMINATED', 'DISQUALIFIED', 'WITHDRAWN'))
);

-- ============================================================
-- 9. TEAM_MEMBERS (giữ nguyên)
-- ============================================================
CREATE TABLE `team_members` (
  `team_id`   BIGINT NOT NULL,
  `user_id`   BIGINT NOT NULL,
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`, `user_id`)
);

-- ============================================================
-- 10. CHECK_INS (MỚI)
--     Độc lập với flow thi thố — chỉ dùng để xác nhận có mặt.
--     Staff check-in từng thành viên khi đến event.
--     KHÔNG tự động ảnh hưởng registration hay submission.
-- ============================================================
CREATE TABLE `check_ins` (
  `checkin_id`  BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`    BIGINT NOT NULL,
  `team_id`     BIGINT NOT NULL,
  `user_id`     BIGINT NOT NULL   COMMENT 'Thành viên được check-in',
  `checked_by`  BIGINT NOT NULL   COMMENT 'Staff thực hiện check-in',
  `checked_in`  TINYINT(1) NOT NULL DEFAULT 0,
  `checked_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uq_checkin` (`event_id`, `team_id`, `user_id`)
  COMMENT 'Mỗi thành viên chỉ check-in 1 lần mỗi event'
);

-- ============================================================
-- 11. TEAM_REGISTRATIONS
--     PENDING   → mới đăng ký, chờ duyệt
--     APPROVED  → được tham gia
--     REJECTED  → bị từ chối đăng ký
--     SUSPENDED → bị loại trong quá trình thi (dùng thay vì xoá)
-- ============================================================
CREATE TABLE `team_registrations` (
  `registration_id` BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`        BIGINT NOT NULL,
  `category_id`     BIGINT NOT NULL,
  `team_id`         BIGINT NOT NULL,
  `status`          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `registered_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`registration_id`),
  CONSTRAINT `chk_tr_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
);

-- ============================================================
-- 12. GROUP_TEAMS (MỚI: team được phân vào bảng nào)
-- ============================================================
CREATE TABLE `group_teams` (
  `group_id`    BIGINT NOT NULL,
  `team_id`     BIGINT NOT NULL,
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`, `team_id`)
);

-- ============================================================
-- 13. SUBMISSIONS (sửa: thêm group_id)
-- ============================================================
CREATE TABLE `submissions` (
  `submission_id`       BIGINT AUTO_INCREMENT NOT NULL,
  `team_id`             BIGINT NOT NULL,
  `round_id`            BIGINT NOT NULL,
  `group_id`            BIGINT NULL COMMENT 'Bảng thi (NULL nếu không chia bảng)',
  `github_url`          LONGTEXT NULL,
  `demo_url`            LONGTEXT NULL,
  `report_url`          LONGTEXT NULL,
  `slide_url`           LONGTEXT NULL,
  `repository_metadata` LONGTEXT NULL,
  `status`              VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
  `submitted_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`submission_id`),
  CONSTRAINT `chk_sub_status` CHECK (`status` IN ('SUBMITTED', 'LATE', 'DISQUALIFIED'))
);

-- ============================================================
-- 14. EVENT_CRITERIA (giữ nguyên)
-- ============================================================
CREATE TABLE `event_criteria` (
  `criteria_id`    BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`       BIGINT NOT NULL,
  `criterion_name` VARCHAR(100) NOT NULL,
  `weight`         DECIMAL(5,2) NOT NULL,
  `max_score`      DECIMAL(5,2) NOT NULL,
  `description`    LONGTEXT NULL,
  `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`criteria_id`)
);

-- ============================================================
-- 15. SCORES (sửa: thêm group_id)
-- ============================================================
CREATE TABLE `scores` (
  `score_id`     BIGINT AUTO_INCREMENT NOT NULL,
  `submission_id` BIGINT NOT NULL,
  `judge_id`     BIGINT NOT NULL,
  `group_id`     BIGINT NULL COMMENT 'Bảng thi (để lọc điểm theo bảng)',
  `total_score`  DECIMAL(6,2) NULL,
  `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`score_id`)
);

CREATE TABLE `score_details` (
  `detail_id`   BIGINT AUTO_INCREMENT NOT NULL,
  `score_id`    BIGINT NOT NULL,
  `criteria_id` BIGINT NOT NULL,
  `score`       DECIMAL(5,2) NOT NULL,
  `feedback`    LONGTEXT NULL,
  PRIMARY KEY (`detail_id`)
);

-- ============================================================
-- 16. GROUP_WINNERS (MỚI: winner từng bảng)
-- ============================================================
CREATE TABLE `group_winners` (
  `winner_id`   BIGINT AUTO_INCREMENT NOT NULL,
  `group_id`    BIGINT NOT NULL,
  `team_id`     BIGINT NOT NULL,
  `rank`        INT NOT NULL    COMMENT 'Thứ hạng trong bảng',
  `total_score` DECIMAL(6,2) NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`winner_id`),
  UNIQUE KEY `uq_group_winner` (`group_id`, `team_id`)
);

-- ============================================================
-- 17. ROUND_WINNERS (MỚI: tổng hợp winner cả vòng + advance)
--     staff_confirmed = 0: chỉ là gợi ý từ system
--     staff_confirmed = 1: staff đã xác nhận danh sách này
-- ============================================================
CREATE TABLE `round_winners` (
  `round_winner_id`     BIGINT AUTO_INCREMENT NOT NULL,
  `round_id`            BIGINT NOT NULL,
  `team_id`             BIGINT NOT NULL,
  `rank`                INT NULL,
  `total_score`         DECIMAL(6,2) NULL,
  `advanced_to_round_id` BIGINT NULL   COMMENT 'Round tiếp theo team được advance',
  `staff_confirmed`     TINYINT(1) NOT NULL DEFAULT 0
                        COMMENT '0 = gợi ý tự động, 1 = staff đã confirm',
  `confirmed_by`        BIGINT NULL    COMMENT 'user_id của staff confirm',
  `created_at`          DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_winner_id`),
  UNIQUE KEY `uq_round_winner` (`round_id`, `team_id`)
);

-- ============================================================
-- 18. JUDGE_ASSIGNMENTS (sửa: thêm group_id)
-- ============================================================
CREATE TABLE `judge_assignments` (
  `assignment_id` BIGINT AUTO_INCREMENT NOT NULL,
  `judge_id`      BIGINT NOT NULL,
  `round_id`      BIGINT NOT NULL,
  `category_id`   BIGINT NOT NULL,
  `group_id`      BIGINT NULL COMMENT 'Phân công vào bảng cụ thể',
  `assigned_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`)
);

-- ============================================================
-- 19. CÁC BẢNG GIỮ NGUYÊN
-- ============================================================
CREATE TABLE `announcements` (
  `announcement_id` BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`        BIGINT NOT NULL,
  `title`           VARCHAR(200) NOT NULL,
  `content`         LONGTEXT NOT NULL,
  `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`announcement_id`)
);

CREATE TABLE `awards` (
  `award_id`   BIGINT AUTO_INCREMENT NOT NULL,
  `event_id`   BIGINT NOT NULL,
  `team_id`    BIGINT NOT NULL,
  `title`      VARCHAR(100) NOT NULL,
  `rank`       INT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`award_id`)
);

CREATE TABLE `audit_logs` (
  `log_id`      BIGINT AUTO_INCREMENT NOT NULL,
  `user_id`     BIGINT NOT NULL,
  `action`      VARCHAR(100) NOT NULL,
  `entity_type` VARCHAR(100) NULL,
  `entity_id`   BIGINT NULL,
  `description` LONGTEXT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`)
);

CREATE TABLE `eliminations` (
  `elimination_id` BIGINT AUTO_INCREMENT NOT NULL,
  `submission_id`  BIGINT NOT NULL,
  `reason`         LONGTEXT NOT NULL,
  `eliminated_by`  BIGINT NOT NULL,
  `created_at`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`elimination_id`)
);

CREATE TABLE `criteria_templates` (
  `template_id` BIGINT AUTO_INCREMENT NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` LONGTEXT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`)
);

CREATE TABLE `criteria_template_items` (
  `item_id`        BIGINT AUTO_INCREMENT NOT NULL,
  `template_id`    BIGINT NOT NULL,
  `criterion_name` VARCHAR(100) NOT NULL,
  `weight`         DECIMAL(5,2) NOT NULL,
  `max_score`      DECIMAL(5,2) NOT NULL,
  `description`    LONGTEXT NULL,
  PRIMARY KEY (`item_id`)
);

CREATE TABLE `category_mentors` (
  `category_id` BIGINT NOT NULL,
  `mentor_id`   BIGINT NOT NULL,
  `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`, `mentor_id`)
);

-- ============================================================
-- FOREIGN KEYS
-- ============================================================
ALTER TABLE `judge_profiles`         ADD CONSTRAINT `fk_jp_user`             FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `studentProfile`         ADD CONSTRAINT `fk_sp_user`             FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `categories`             ADD CONSTRAINT `fk_cat_event`           FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `rounds`                 ADD CONSTRAINT `fk_rounds_event`        FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `round_groups`           ADD CONSTRAINT `fk_rg_round`            FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `teams`                  ADD CONSTRAINT `fk_teams_leader`        FOREIGN KEY (`leader_id`)             REFERENCES `users`(`user_id`);
ALTER TABLE `team_members`           ADD CONSTRAINT `fk_tm_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `team_members`           ADD CONSTRAINT `fk_tm_user`             FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `check_ins`              ADD CONSTRAINT `fk_ci_event`            FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `check_ins`              ADD CONSTRAINT `fk_ci_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `check_ins`              ADD CONSTRAINT `fk_ci_user`             FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `check_ins`              ADD CONSTRAINT `fk_ci_staff`            FOREIGN KEY (`checked_by`)            REFERENCES `users`(`user_id`);
ALTER TABLE `team_registrations`     ADD CONSTRAINT `fk_tr_event`            FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `team_registrations`     ADD CONSTRAINT `fk_tr_category`         FOREIGN KEY (`category_id`)           REFERENCES `categories`(`category_id`);
ALTER TABLE `team_registrations`     ADD CONSTRAINT `fk_tr_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `group_teams`            ADD CONSTRAINT `fk_gt_group`            FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `group_teams`            ADD CONSTRAINT `fk_gt_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `submissions`            ADD CONSTRAINT `fk_sub_team`            FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `submissions`            ADD CONSTRAINT `fk_sub_round`           FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `submissions`            ADD CONSTRAINT `fk_sub_group`           FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `event_criteria`         ADD CONSTRAINT `fk_ec_event`            FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `scores`                 ADD CONSTRAINT `fk_sc_submission`       FOREIGN KEY (`submission_id`)         REFERENCES `submissions`(`submission_id`);
ALTER TABLE `scores`                 ADD CONSTRAINT `fk_sc_judge`            FOREIGN KEY (`judge_id`)              REFERENCES `users`(`user_id`);
ALTER TABLE `scores`                 ADD CONSTRAINT `fk_sc_group`            FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `score_details`          ADD CONSTRAINT `fk_sd_score`            FOREIGN KEY (`score_id`)              REFERENCES `scores`(`score_id`);
ALTER TABLE `score_details`          ADD CONSTRAINT `fk_sd_criteria`         FOREIGN KEY (`criteria_id`)           REFERENCES `event_criteria`(`criteria_id`);
ALTER TABLE `group_winners`          ADD CONSTRAINT `fk_gw_group`            FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `group_winners`          ADD CONSTRAINT `fk_gw_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `round_winners`          ADD CONSTRAINT `fk_rw_round`            FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `round_winners`          ADD CONSTRAINT `fk_rw_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `round_winners`          ADD CONSTRAINT `fk_rw_next_round`       FOREIGN KEY (`advanced_to_round_id`)  REFERENCES `rounds`(`round_id`);
ALTER TABLE `round_winners`          ADD CONSTRAINT `fk_rw_confirmed_by`     FOREIGN KEY (`confirmed_by`)          REFERENCES `users`(`user_id`);
ALTER TABLE `judge_assignments`      ADD CONSTRAINT `fk_ja_judge`            FOREIGN KEY (`judge_id`)              REFERENCES `users`(`user_id`);
ALTER TABLE `judge_assignments`      ADD CONSTRAINT `fk_ja_round`            FOREIGN KEY (`round_id`)              REFERENCES `rounds`(`round_id`);
ALTER TABLE `judge_assignments`      ADD CONSTRAINT `fk_ja_category`         FOREIGN KEY (`category_id`)           REFERENCES `categories`(`category_id`);
ALTER TABLE `judge_assignments`      ADD CONSTRAINT `fk_ja_group`            FOREIGN KEY (`group_id`)              REFERENCES `round_groups`(`group_id`);
ALTER TABLE `announcements`          ADD CONSTRAINT `fk_ann_event`           FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `awards`                 ADD CONSTRAINT `fk_aw_event`            FOREIGN KEY (`event_id`)              REFERENCES `events`(`event_id`);
ALTER TABLE `awards`                 ADD CONSTRAINT `fk_aw_team`             FOREIGN KEY (`team_id`)               REFERENCES `teams`(`team_id`);
ALTER TABLE `audit_logs`             ADD CONSTRAINT `fk_al_user`             FOREIGN KEY (`user_id`)               REFERENCES `users`(`user_id`);
ALTER TABLE `eliminations`           ADD CONSTRAINT `fk_el_submission`       FOREIGN KEY (`submission_id`)         REFERENCES `submissions`(`submission_id`);
ALTER TABLE `eliminations`           ADD CONSTRAINT `fk_el_user`             FOREIGN KEY (`eliminated_by`)         REFERENCES `users`(`user_id`);
ALTER TABLE `criteria_template_items` ADD CONSTRAINT `fk_cti_template`       FOREIGN KEY (`template_id`)           REFERENCES `criteria_templates`(`template_id`);
ALTER TABLE `category_mentors`       ADD CONSTRAINT `fk_cm_category`         FOREIGN KEY (`category_id`)           REFERENCES `categories`(`category_id`);
ALTER TABLE `category_mentors`       ADD CONSTRAINT `fk_cm_mentor`           FOREIGN KEY (`mentor_id`)             REFERENCES `users`(`user_id`);
