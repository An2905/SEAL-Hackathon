-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: hackathon
-- ------------------------------------------------------
-- Server version	9.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'b424a162-5f1e-11f1-82ff-dc4628233c63:1-513';

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `announcement_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` longtext NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`announcement_id`),
  KEY `idx_ann_event` (`event_id`),
  CONSTRAINT `fk_ann_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `log_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `action` varchar(100) NOT NULL,
  `entity_type` varchar(100) DEFAULT NULL,
  `entity_id` varchar(36) DEFAULT NULL,
  `description` longtext,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_al_user` (`user_id`),
  CONSTRAINT `fk_al_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `awards`
--

DROP TABLE IF EXISTS `awards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `awards` (
  `award_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `team_id` varchar(36) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `rank` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`award_id`),
  KEY `idx_aw_event` (`event_id`),
  KEY `fk_aw_team` (`team_id`),
  CONSTRAINT `fk_aw_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_aw_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `message_id` varchar(36) NOT NULL,
  `room_id` varchar(36) NOT NULL,
  `sender_id` varchar(36) NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_cm_room` (`room_id`),
  KEY `idx_cm_room_created` (`room_id`,`created_at`),
  KEY `fk_cm_sender` (`sender_id`),
  CONSTRAINT `fk_cm_room` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`room_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cm_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_room_members`
--

DROP TABLE IF EXISTS `chat_room_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_room_members` (
  `room_member_id` varchar(36) NOT NULL,
  `room_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`room_member_id`),
  UNIQUE KEY `uq_crm_room_user` (`room_id`,`user_id`),
  KEY `idx_crm_user` (`user_id`),
  CONSTRAINT `fk_crm_room` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`room_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_crm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_rooms`
--

DROP TABLE IF EXISTS `chat_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_rooms` (
  `room_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `mentor_id` varchar(36) NOT NULL,
  `created_by` varchar(36) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `closed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`room_id`),
  UNIQUE KEY `uq_chat_room` (`team_id`,`mentor_id`,`round_id`),
  KEY `idx_cr_event` (`event_id`),
  KEY `idx_cr_group` (`group_id`),
  KEY `idx_cr_mentor` (`mentor_id`),
  KEY `fk_cr_round` (`round_id`),
  KEY `fk_cr_created_by` (`created_by`),
  CONSTRAINT `fk_cr_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_cr_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_cr_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_cr_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_cr_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_cr_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `chk_cr_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'CLOSED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `check_ins`
--

DROP TABLE IF EXISTS `check_ins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `check_ins` (
  `checkin_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `checked_by` varchar(36) NOT NULL,
  `checked_in` tinyint(1) NOT NULL DEFAULT '0',
  `checked_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uq_checkin` (`event_id`,`team_id`,`user_id`),
  KEY `idx_ci_team` (`team_id`),
  KEY `fk_ci_user` (`user_id`),
  KEY `fk_ci_staff` (`checked_by`),
  CONSTRAINT `fk_ci_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_ci_staff` FOREIGN KEY (`checked_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_ci_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `fk_ci_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `criteria_template_items`
--

DROP TABLE IF EXISTS `criteria_template_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `criteria_template_items` (
  `item_id` varchar(36) NOT NULL,
  `template_id` varchar(36) NOT NULL,
  `criterion_name` varchar(100) NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `max_score` decimal(5,2) NOT NULL,
  `description` longtext,
  PRIMARY KEY (`item_id`),
  KEY `idx_cti_template` (`template_id`),
  CONSTRAINT `fk_cti_template` FOREIGN KEY (`template_id`) REFERENCES `criteria_templates` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `criteria_templates`
--

DROP TABLE IF EXISTS `criteria_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `criteria_templates` (
  `template_id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` longtext,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `eliminations`
--

DROP TABLE IF EXISTS `eliminations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eliminations` (
  `elimination_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `reason` longtext NOT NULL,
  `eliminated_by` varchar(36) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`elimination_id`),
  KEY `idx_el_team` (`team_id`),
  KEY `idx_el_event` (`event_id`),
  KEY `fk_el_user` (`eliminated_by`),
  CONSTRAINT `fk_el_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_el_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `fk_el_user` FOREIGN KEY (`eliminated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_criteria`
-- Scoring criteria belong to a round (not the whole event).
--

DROP TABLE IF EXISTS `event_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_criteria` (
  `criteria_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `criterion_name` varchar(100) NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `max_score` decimal(5,2) NOT NULL,
  `description` longtext,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`criteria_id`),
  KEY `idx_ec_round` (`round_id`),
  CONSTRAINT `fk_ec_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `events`
--

DROP TABLE IF EXISTS `events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `events` (
  `event_id` varchar(36) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` longtext,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'BUILDING',
  `max_teams` int DEFAULT NULL,
  `num_rounds` int NOT NULL DEFAULT '1',
  `github_template_repo` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  CONSTRAINT `chk_events_status` CHECK ((`status` in (_utf8mb3'BUILDING',_utf8mb3'UPCOMING',_utf8mb3'ONGOING',_utf8mb3'COMPLETED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_teams`
--

DROP TABLE IF EXISTS `group_teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_teams` (
  `group_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`,`team_id`),
  UNIQUE KEY `uq_gt_team_round` (`team_id`,`round_id`),
  KEY `idx_gt_team` (`team_id`),
  KEY `idx_gt_round` (`round_id`),
  CONSTRAINT `fk_gt_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_gt_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_gt_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_winners`
--

DROP TABLE IF EXISTS `group_winners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `group_winners` (
  `winner_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `rank` int NOT NULL,
  `total_score` decimal(6,2) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`winner_id`),
  UNIQUE KEY `uq_group_winner` (`group_id`,`team_id`),
  KEY `idx_gw_team` (`team_id`),
  CONSTRAINT `fk_gw_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_gw_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_assignments`
--

DROP TABLE IF EXISTS `judge_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_assignments` (
  `assignment_id` varchar(36) NOT NULL,
  `judge_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uq_ja_judge_round_group` (`judge_id`,`round_id`,`group_id`),
  KEY `idx_ja_round` (`round_id`),
  KEY `idx_ja_group` (`group_id`),
  CONSTRAINT `fk_ja_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_ja_judge` FOREIGN KEY (`judge_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_ja_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_team_assignments`
--

DROP TABLE IF EXISTS `judge_team_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_team_assignments` (
  `assignment_id` varchar(36) NOT NULL,
  `judge_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uq_jta_round_team` (`round_id`,`team_id`),
  UNIQUE KEY `uq_jta_judge_round_team` (`judge_id`,`round_id`,`team_id`),
  KEY `idx_jta_judge` (`judge_id`),
  KEY `idx_jta_group` (`group_id`),
  CONSTRAINT `fk_jta_judge` FOREIGN KEY (`judge_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_jta_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_jta_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_jta_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mentor_assignments`
--

DROP TABLE IF EXISTS `mentor_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mentor_assignments` (
  `assignment_id` varchar(36) NOT NULL,
  `mentor_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  UNIQUE KEY `uq_ma_mentor_round_group` (`mentor_id`,`round_id`,`group_id`),
  KEY `idx_ma_round` (`round_id`),
  KEY `idx_ma_group` (`group_id`),
  CONSTRAINT `fk_ma_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_ma_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_ma_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `participants_profile`
--

DROP TABLE IF EXISTS `participants_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `participants_profile` (
  `user_id` varchar(36) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
  `organization` varchar(200) DEFAULT NULL,
  `participant_type` varchar(20) NOT NULL DEFAULT 'INTERNAL',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_pp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_pp_type` CHECK ((`participant_type` in (_utf8mb4'INTERNAL',_utf8mb4'EXTERNAL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_groups`
--

DROP TABLE IF EXISTS `round_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_groups` (
  `group_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `max_teams` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`),
  KEY `idx_rg_round` (`round_id`),
  CONSTRAINT `fk_rg_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `round_winners`
--

DROP TABLE IF EXISTS `round_winners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `round_winners` (
  `round_winner_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `rank` int DEFAULT NULL,
  `total_score` decimal(6,2) DEFAULT NULL,
  `advanced_to_round_id` varchar(36) DEFAULT NULL,
  `staff_confirmed` tinyint(1) NOT NULL DEFAULT '0',
  `confirmed_by` varchar(36) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_winner_id`),
  UNIQUE KEY `uq_round_winner` (`round_id`,`team_id`),
  KEY `idx_rw_team` (`team_id`),
  KEY `fk_rw_next_round` (`advanced_to_round_id`),
  KEY `fk_rw_confirmed_by` (`confirmed_by`),
  CONSTRAINT `fk_rw_confirmed_by` FOREIGN KEY (`confirmed_by`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_rw_next_round` FOREIGN KEY (`advanced_to_round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_rw_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_rw_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rounds`
--

DROP TABLE IF EXISTS `rounds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rounds` (
  `round_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `name` varchar(100) NOT NULL,
  `round_order` int NOT NULL,
  `num_groups` int NOT NULL DEFAULT '1',
  `max_teams_per_group` int DEFAULT NULL,
  `winners_per_round` int NOT NULL DEFAULT '1',
  `submission_deadline` datetime DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_id`),
  KEY `idx_rounds_event` (`event_id`),
  CONSTRAINT `fk_rounds_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `score_details`
--

DROP TABLE IF EXISTS `score_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `score_details` (
  `detail_id` varchar(36) NOT NULL,
  `score_id` varchar(36) NOT NULL,
  `criteria_id` varchar(36) NOT NULL,
  `score` decimal(5,2) NOT NULL,
  `feedback` longtext,
  PRIMARY KEY (`detail_id`),
  KEY `idx_sd_score` (`score_id`),
  KEY `idx_sd_criteria` (`criteria_id`),
  CONSTRAINT `fk_sd_criteria` FOREIGN KEY (`criteria_id`) REFERENCES `event_criteria` (`criteria_id`),
  CONSTRAINT `fk_sd_score` FOREIGN KEY (`score_id`) REFERENCES `scores` (`score_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores`
--

DROP TABLE IF EXISTS `scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scores` (
  `score_id` varchar(36) NOT NULL,
  `submission_id` varchar(36) NOT NULL,
  `judge_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `total_score` decimal(6,2) DEFAULT NULL,
  `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`score_id`),
  UNIQUE KEY `uq_sc_submission_judge` (`submission_id`,`judge_id`),
  KEY `idx_sc_judge` (`judge_id`),
  KEY `idx_sc_group` (`group_id`),
  CONSTRAINT `fk_sc_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_sc_judge` FOREIGN KEY (`judge_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_sc_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `studentprofile`
--

DROP TABLE IF EXISTS `studentprofile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `studentprofile` (
  `user_id` varchar(36) NOT NULL,
  `student_code` varchar(30) DEFAULT NULL,
  `university_name` varchar(150) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_sp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submissions`
--

DROP TABLE IF EXISTS `submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submissions` (
  `submission_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `round_id` varchar(36) NOT NULL,
  `group_id` varchar(36) NOT NULL,
  `github_url` longtext,
  `demo_url` longtext,
  `report_url` longtext,
  `slide_url` longtext,
  `repository_metadata` longtext,
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED',
  `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`submission_id`),
  UNIQUE KEY `uq_sub_team_round` (`team_id`,`round_id`),
  KEY `idx_sub_round` (`round_id`),
  KEY `idx_sub_group` (`group_id`),
  CONSTRAINT `fk_sub_group` FOREIGN KEY (`group_id`) REFERENCES `round_groups` (`group_id`),
  CONSTRAINT `fk_sub_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_sub_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `chk_sub_status` CHECK ((`status` in (_utf8mb4'SUBMITTED',_utf8mb4'LATE',_utf8mb4'DISQUALIFIED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team_members`
--

DROP TABLE IF EXISTS `team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_members` (
  `team_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`,`user_id`),
  KEY `idx_tm_user` (`user_id`),
  CONSTRAINT `fk_tm_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `fk_tm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team_registrations`
--

DROP TABLE IF EXISTS `team_registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_registrations` (
  `registration_id` varchar(36) NOT NULL,
  `event_id` varchar(36) NOT NULL,
  `team_id` varchar(36) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `registered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `github_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `github_repo_id` bigint DEFAULT NULL,
  `github_repo_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`registration_id`),
  UNIQUE KEY `uq_tr_event_team` (`event_id`,`team_id`),
  KEY `idx_tr_team` (`team_id`),
  CONSTRAINT `fk_tr_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_tr_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `chk_tr_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'SUSPENDED',_utf8mb4'DROPPED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teams`
--

DROP TABLE IF EXISTS `teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams` (
  `team_id` varchar(36) NOT NULL,
  `team_name` varchar(100) NOT NULL,
  `leader_id` varchar(36) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `max_members` int NOT NULL DEFAULT '5',
  `enrollCode` varchar(50) NOT NULL DEFAULT 'TEMP_CODE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `uq_team_name` (`team_name`),
  KEY `idx_teams_leader` (`leader_id`),
  CONSTRAINT `fk_teams_leader` FOREIGN KEY (`leader_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_teams_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'ELIMINATED',_utf8mb4'DISQUALIFIED',_utf8mb4'WITHDRAWN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `universities`
--

DROP TABLE IF EXISTS `universities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universities` (
  `university_id` varchar(36) NOT NULL,
  `university_name` varchar(255) NOT NULL,
  PRIMARY KEY (`university_id`),
  UNIQUE KEY `uq_uni_name` (`university_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` varchar(36) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `password_hash` longtext,
  `role` varchar(30) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `github_username` varchar(100) DEFAULT NULL,
  `github_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `chk_users_role` CHECK ((`role` in (_utf8mb3'COORDINATOR',_utf8mb3'EXPERT_INTERNAL',_utf8mb3'EXPERT_EXTERNAL',_utf8mb3'STUDENT_FPT',_utf8mb3'STUDENT_EXTERNAL'))),
  CONSTRAINT `chk_users_status` CHECK ((`status` in (_utf8mb3'PENDING',_utf8mb3'APPROVED',_utf8mb3'REJECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-07 13:45:42
