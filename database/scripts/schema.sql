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
-- Table structure for table `advancement_rules`
--

DROP TABLE IF EXISTS `advancement_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `advancement_rules` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT,
  `round_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `top_n` int NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`rule_id`),
  KEY `fk_adv_rules_category` (`category_id`),
  KEY `fk_adv_rules_round` (`round_id`),
  CONSTRAINT `fk_adv_rules_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `fk_adv_rules_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `announcement_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` longtext NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`announcement_id`),
  KEY `fk_announcements_event` (`event_id`),
  CONSTRAINT `fk_announcements_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `action` varchar(100) NOT NULL,
  `entity_type` varchar(100) DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `description` longtext,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `fk_audit_logs_user` (`user_id`),
  CONSTRAINT `fk_audit_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `awards`
--

DROP TABLE IF EXISTS `awards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `awards` (
  `award_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `team_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `rank` int DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`award_id`),
  KEY `fk_awards_event` (`event_id`),
  KEY `fk_awards_team` (`team_id`),
  CONSTRAINT `fk_awards_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_awards_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `calibration_rounds`
--

DROP TABLE IF EXISTS `calibration_rounds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calibration_rounds` (
  `calibration_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`calibration_id`),
  KEY `fk_cal_rounds_event` (`event_id`),
  CONSTRAINT `fk_cal_rounds_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `calibration_scores`
--

DROP TABLE IF EXISTS `calibration_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `calibration_scores` (
  `calibration_score_id` bigint NOT NULL AUTO_INCREMENT,
  `calibration_id` bigint NOT NULL,
  `judge_id` bigint NOT NULL,
  `criteria_id` bigint NOT NULL,
  `score` decimal(5,2) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`calibration_score_id`),
  KEY `fk_cal_scores_cal` (`calibration_id`),
  KEY `fk_cal_scores_criteria` (`criteria_id`),
  KEY `fk_cal_scores_judge` (`judge_id`),
  CONSTRAINT `fk_cal_scores_cal` FOREIGN KEY (`calibration_id`) REFERENCES `calibration_rounds` (`calibration_id`),
  CONSTRAINT `fk_cal_scores_criteria` FOREIGN KEY (`criteria_id`) REFERENCES `event_criteria` (`criteria_id`),
  CONSTRAINT `fk_cal_scores_judge` FOREIGN KEY (`judge_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` longtext,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  KEY `fk_categories_event` (`event_id`),
  CONSTRAINT `fk_categories_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category_mentors`
--

DROP TABLE IF EXISTS `category_mentors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_mentors` (
  `category_id` bigint NOT NULL,
  `mentor_id` bigint NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`,`mentor_id`),
  KEY `fk_cat_mentors_mentor` (`mentor_id`),
  CONSTRAINT `fk_cat_mentors_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `fk_cat_mentors_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `criteria_template_items`
--

DROP TABLE IF EXISTS `criteria_template_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `criteria_template_items` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `criterion_name` varchar(100) NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `max_score` decimal(5,2) NOT NULL,
  `description` longtext,
  PRIMARY KEY (`item_id`),
  KEY `fk_cti_template` (`template_id`),
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
  `template_id` bigint NOT NULL AUTO_INCREMENT,
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
  `elimination_id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `reason` longtext NOT NULL,
  `eliminated_by` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`elimination_id`),
  KEY `fk_eliminations_user` (`eliminated_by`),
  KEY `fk_eliminations_submission` (`submission_id`),
  CONSTRAINT `fk_eliminations_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`submission_id`),
  CONSTRAINT `fk_eliminations_user` FOREIGN KEY (`eliminated_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_criteria`
--

DROP TABLE IF EXISTS `event_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_criteria` (
  `criteria_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `criterion_name` varchar(100) NOT NULL,
  `weight` decimal(5,2) NOT NULL,
  `max_score` decimal(5,2) NOT NULL,
  `description` longtext,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`criteria_id`),
  KEY `fk_event_criteria_event` (`event_id`),
  CONSTRAINT `fk_event_criteria_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `events`
--

DROP TABLE IF EXISTS `events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `events` (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `description` longtext,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'UPCOMING',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  CONSTRAINT `chk_events_status` CHECK ((`status` in (_utf8mb4'UPCOMING',_utf8mb4'ONGOING',_utf8mb4'COMPLETED')))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `judge_assignments`
--

DROP TABLE IF EXISTS `judge_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `judge_assignments` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT,
  `judge_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `assigned_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`assignment_id`),
  KEY `fk_judge_assign_category` (`category_id`),
  KEY `fk_judge_assign_judge` (`judge_id`),
  KEY `fk_judge_assign_round` (`round_id`),
  CONSTRAINT `fk_judge_assign_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `fk_judge_assign_judge` FOREIGN KEY (`judge_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_judge_assign_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rounds`
--

DROP TABLE IF EXISTS `rounds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rounds` (
  `round_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `round_order` int NOT NULL,
  `submission_deadline` datetime DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`round_id`),
  KEY `fk_rounds_event` (`event_id`),
  CONSTRAINT `fk_rounds_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `score_details`
--

DROP TABLE IF EXISTS `score_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `score_details` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `score_id` bigint NOT NULL,
  `criteria_id` bigint NOT NULL,
  `score` decimal(5,2) NOT NULL,
  `feedback` longtext,
  PRIMARY KEY (`detail_id`),
  KEY `fk_score_details_criteria` (`criteria_id`),
  KEY `fk_score_details_score` (`score_id`),
  CONSTRAINT `fk_score_details_criteria` FOREIGN KEY (`criteria_id`) REFERENCES `event_criteria` (`criteria_id`),
  CONSTRAINT `fk_score_details_score` FOREIGN KEY (`score_id`) REFERENCES `scores` (`score_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scores`
--

DROP TABLE IF EXISTS `scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scores` (
  `score_id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `judge_id` bigint NOT NULL,
  `total_score` decimal(6,2) DEFAULT NULL,
  `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`score_id`),
  KEY `fk_scores_judge` (`judge_id`),
  KEY `fk_scores_submission` (`submission_id`),
  CONSTRAINT `fk_scores_judge` FOREIGN KEY (`judge_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_scores_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `studentprofile`
--

DROP TABLE IF EXISTS `studentprofile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `studentprofile` (
  `profile_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `student_code` varchar(30) DEFAULT NULL,
  `university_name` varchar(150) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`),
  KEY `fk_student_profile_user` (`user_id`),
  CONSTRAINT `fk_student_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submissions`
--

DROP TABLE IF EXISTS `submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submissions` (
  `submission_id` bigint NOT NULL AUTO_INCREMENT,
  `team_id` bigint NOT NULL,
  `round_id` bigint NOT NULL,
  `github_url` longtext,
  `demo_url` longtext,
  `report_url` longtext,
  `slide_url` longtext,
  `repository_metadata` longtext,
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED',
  `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`submission_id`),
  KEY `fk_submissions_round` (`round_id`),
  KEY `fk_submissions_team` (`team_id`),
  CONSTRAINT `fk_submissions_round` FOREIGN KEY (`round_id`) REFERENCES `rounds` (`round_id`),
  CONSTRAINT `fk_submissions_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `chk_submissions_status` CHECK ((`status` in (_utf8mb4'SUBMITTED',_utf8mb4'LATE',_utf8mb4'DISQUALIFIED')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team_members`
--

DROP TABLE IF EXISTS `team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_members` (
  `team_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`team_id`,`user_id`),
  KEY `fk_team_members_user` (`user_id`),
  CONSTRAINT `fk_team_members_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `fk_team_members_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team_registrations`
--

DROP TABLE IF EXISTS `team_registrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_registrations` (
  `registration_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `team_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `registered_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`registration_id`),
  KEY `fk_team_reg_category` (`category_id`),
  KEY `fk_team_reg_event` (`event_id`),
  KEY `fk_team_reg_team` (`team_id`),
  CONSTRAINT `fk_team_reg_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),
  CONSTRAINT `fk_team_reg_event` FOREIGN KEY (`event_id`) REFERENCES `events` (`event_id`),
  CONSTRAINT `fk_team_reg_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`team_id`),
  CONSTRAINT `chk_team_reg_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED')))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `teams`
--

DROP TABLE IF EXISTS `teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams` (
  `team_id` bigint NOT NULL AUTO_INCREMENT,
  `team_name` varchar(100) NOT NULL,
  `leader_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `enrollCode` varchar(50) NOT NULL DEFAULT 'TEMP_CODE',
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `uq_teams_team_name` (`team_name`),
  KEY `fk_teams_leader` (`leader_id`),
  CONSTRAINT `fk_teams_leader` FOREIGN KEY (`leader_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `chk_teams_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'ELIMINATED',_utf8mb4'DISQUALIFIED',_utf8mb4'WITHDRAWN')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `universities`
--

DROP TABLE IF EXISTS `universities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `universities` (
  `university_id` bigint NOT NULL AUTO_INCREMENT,
  `university_name` varchar(255) NOT NULL,
  PRIMARY KEY (`university_id`),
  UNIQUE KEY `uq_universities_name` (`university_name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `password_hash` longtext,
  `role` varchar(50) NOT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `chk_users_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'APPROVED',_utf8mb4'REJECTED')))
) ENGINE=InnoDB AUTO_INCREMENT=10035 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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

-- Dump completed on 2026-06-03 16:00:03
