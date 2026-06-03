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

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'b424a162-5f1e-11f1-82ff-dc4628233c63:1-110';

--
-- Dumping data for table `advancement_rules`
--

LOCK TABLES `advancement_rules` WRITE;
/*!40000 ALTER TABLE `advancement_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `advancement_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
INSERT INTO `announcements` VALUES (1,1,'Welcome to FPT AI Hackathon 2026','Registration is now officially open.','2026-05-27 17:11:33'),(2,1,'Semi Final Schedule','The semi final round will start on June 10.','2026-05-27 17:11:33'),(3,2,'Science Event Opening','Welcome all participants to FPT Science.','2026-05-27 17:11:33'),(4,2,'Presentation Reminder','Please prepare your presentation slides carefully.','2026-05-27 17:11:33'),(5,3,'FPT Tech Launch','Prototype submissions are required before August 5.','2026-05-27 17:11:33'),(6,3,'Final Evaluation','Final judging session will be held onsite.','2026-05-27 17:11:33'),(7,1,'123','456','2026-05-31 17:21:59'),(8,1,'123','456','2026-05-31 17:23:17'),(9,1,'123','456','2026-05-31 17:25:33');
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `awards`
--

LOCK TABLES `awards` WRITE;
/*!40000 ALTER TABLE `awards` DISABLE KEYS */;
INSERT INTO `awards` VALUES (2,1,1,'Best Innovation',1,'2026-05-27 17:14:43'),(3,2,3,'Best Science Project',1,'2026-05-27 17:14:43'),(4,3,5,'Best Technology Solution',1,'2026-05-27 17:14:43');
/*!40000 ALTER TABLE `awards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `calibration_rounds`
--

LOCK TABLES `calibration_rounds` WRITE;
/*!40000 ALTER TABLE `calibration_rounds` DISABLE KEYS */;
/*!40000 ALTER TABLE `calibration_rounds` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `calibration_scores`
--

LOCK TABLES `calibration_scores` WRITE;
/*!40000 ALTER TABLE `calibration_scores` DISABLE KEYS */;
/*!40000 ALTER TABLE `calibration_scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES (1,1,'Mobile App','Mobile development projects','2026-05-27 17:11:33'),(2,1,'Cyber Security','Security and privacy solutions','2026-05-27 17:11:33'),(3,2,'IoT','Internet of Things projects','2026-05-27 17:11:33'),(4,2,'Data Science','Big data and analytics','2026-05-27 17:11:33'),(5,3,'Cloud Computing','Cloud native systems','2026-05-27 17:11:33'),(6,3,'Game Development','Game and interactive applications','2026-05-27 17:11:33');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `category_mentors`
--

LOCK TABLES `category_mentors` WRITE;
/*!40000 ALTER TABLE `category_mentors` DISABLE KEYS */;
INSERT INTO `category_mentors` VALUES (1,2,'2026-05-27 17:14:43'),(2,43,'2026-05-27 17:14:43'),(3,44,'2026-05-27 17:14:43'),(4,2,'2026-05-27 17:14:43'),(5,43,'2026-05-27 17:14:43');
/*!40000 ALTER TABLE `category_mentors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `criteria_template_items`
--

LOCK TABLES `criteria_template_items` WRITE;
/*!40000 ALTER TABLE `criteria_template_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `criteria_template_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `criteria_templates`
--

LOCK TABLES `criteria_templates` WRITE;
/*!40000 ALTER TABLE `criteria_templates` DISABLE KEYS */;
/*!40000 ALTER TABLE `criteria_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `eliminations`
--

LOCK TABLES `eliminations` WRITE;
/*!40000 ALTER TABLE `eliminations` DISABLE KEYS */;
/*!40000 ALTER TABLE `eliminations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `event_criteria`
--

LOCK TABLES `event_criteria` WRITE;
/*!40000 ALTER TABLE `event_criteria` DISABLE KEYS */;
INSERT INTO `event_criteria` VALUES (1,1,'Innovation',30.00,10.00,'Creativity and uniqueness','2026-05-27 17:11:33'),(2,1,'Technical Skill',40.00,10.00,'Technical implementation','2026-05-27 17:11:33'),(3,1,'Presentation',30.00,10.00,'Presentation quality','2026-05-27 17:11:33'),(4,2,'Business Value',35.00,10.00,'Real world impact','2026-05-27 17:11:33'),(5,2,'Scalability',35.00,10.00,'Scalable architecture','2026-05-27 17:11:33'),(6,2,'UI UX',30.00,10.00,'User experience','2026-05-27 17:11:33'),(7,3,'Performance',40.00,10.00,'System performance','2026-05-27 17:11:33'),(8,3,'Design',30.00,10.00,'System design quality','2026-05-27 17:11:33'),(9,3,'Completeness',30.00,10.00,'Feature completeness','2026-05-27 17:11:33');
/*!40000 ALTER TABLE `event_criteria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `events`
--

LOCK TABLES `events` WRITE;
/*!40000 ALTER TABLE `events` DISABLE KEYS */;
INSERT INTO `events` VALUES (1,'FPT AI Hackathon 2026','AI innovation competition','2026-06-01 00:00:00','2026-06-30 00:00:00','ONGOING','2026-05-19 16:05:44'),(2,'FPT Sience','AI innovation competition','2026-06-01 00:00:00','2026-06-30 00:00:00','ONGOING','2026-05-19 16:05:52'),(3,'FPT Tech','AI innovation competition','2026-06-01 00:00:00','2026-06-30 00:00:00','ONGOING','2026-05-19 17:16:36');
/*!40000 ALTER TABLE `events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `judge_assignments`
--

LOCK TABLES `judge_assignments` WRITE;
/*!40000 ALTER TABLE `judge_assignments` DISABLE KEYS */;
INSERT INTO `judge_assignments` VALUES (5,3,1,1,'2026-05-27 17:14:43'),(6,45,2,2,'2026-05-27 17:14:43'),(7,46,3,1,'2026-05-27 17:14:43'),(8,3,4,3,'2026-05-27 17:14:43'),(9,45,5,4,'2026-05-27 17:14:43'),(10,46,6,5,'2026-05-27 17:14:43'),(11,45,7,6,'2026-05-27 17:14:43'),(12,41,1,1,'2026-06-01 07:51:08');
/*!40000 ALTER TABLE `judge_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `rounds`
--

LOCK TABLES `rounds` WRITE;
/*!40000 ALTER TABLE `rounds` DISABLE KEYS */;
INSERT INTO `rounds` VALUES (1,1,'Idea Round',1,'2026-06-05 00:00:00','2026-06-01 00:00:00','2026-06-05 00:00:00','2026-05-27 17:11:33'),(2,1,'Semi Final',2,'2026-06-15 00:00:00','2026-06-10 00:00:00','2026-06-15 00:00:00','2026-05-27 17:11:33'),(3,1,'Final Round',3,'2026-06-28 00:00:00','2026-06-25 00:00:00','2026-06-28 00:00:00','2026-05-27 17:11:33'),(4,2,'Qualification',1,'2026-07-05 00:00:00','2026-07-01 00:00:00','2026-07-05 00:00:00','2026-05-27 17:11:33'),(5,2,'Presentation',2,'2026-07-20 00:00:00','2026-07-15 00:00:00','2026-07-20 00:00:00','2026-05-27 17:11:33'),(6,3,'Prototype Round',1,'2026-08-05 00:00:00','2026-08-01 00:00:00','2026-08-05 00:00:00','2026-05-27 17:11:33'),(7,3,'Grand Final',2,'2026-08-25 00:00:00','2026-08-20 00:00:00','2026-08-25 00:00:00','2026-05-27 17:11:33');
/*!40000 ALTER TABLE `rounds` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `score_details`
--

LOCK TABLES `score_details` WRITE;
/*!40000 ALTER TABLE `score_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `score_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `scores`
--

LOCK TABLES `scores` WRITE;
/*!40000 ALTER TABLE `scores` DISABLE KEYS */;
/*!40000 ALTER TABLE `scores` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `studentprofile`
--

LOCK TABLES `studentprofile` WRITE;
/*!40000 ALTER TABLE `studentprofile` DISABLE KEYS */;
INSERT INTO `studentprofile` VALUES (1,4,'SE182000','FPT University','2026-05-19 17:14:49'),(2,5,'SE182001','HCM University of Technology','2026-05-19 17:14:49'),(3,4,'SE182000','FPT University','2026-05-19 17:16:36'),(4,5,'EXT2026','HCM University of Technology','2026-05-19 17:16:36'),(6,18,'HE199999','FPT University','2026-05-19 17:57:15'),(7,23,'SE123456','FPT HCM','2026-05-19 18:19:18'),(12,28,'FE112233','FPTU','2026-05-19 18:58:07'),(15,31,'BK102304','BKU','2026-05-21 10:58:14'),(17,29,'SE192626','FPT','2026-05-21 21:13:44'),(24,39,'SE205201','FPTU','2026-05-22 00:03:29'),(25,47,'SE123123','FPTU','2026-05-22 19:25:34');
/*!40000 ALTER TABLE `studentprofile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `submissions`
--

LOCK TABLES `submissions` WRITE;
/*!40000 ALTER TABLE `submissions` DISABLE KEYS */;
INSERT INTO `submissions` VALUES (1,6,1,'https://www.youtube.com/watch?v=J6xxOwAQLss','https://www.youtube.com/watch?v=J6xxOwAQLss','https://www.youtube.com/watch?v=J6xxOwAQLss','https://www.youtube.com/watch?v=J6xxOwAQLss','ez asf','SUBMITTED','2026-06-01 16:17:53');
/*!40000 ALTER TABLE `submissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `team_members`
--

LOCK TABLES `team_members` WRITE;
/*!40000 ALTER TABLE `team_members` DISABLE KEYS */;
INSERT INTO `team_members` VALUES (1,4,'2026-05-27 17:14:43'),(2,5,'2026-05-27 17:14:43'),(3,18,'2026-05-27 17:14:43'),(4,23,'2026-05-27 17:14:43'),(5,29,'2026-05-27 17:14:43'),(6,10034,'2026-06-01 16:12:42');
/*!40000 ALTER TABLE `team_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `team_registrations`
--

LOCK TABLES `team_registrations` WRITE;
/*!40000 ALTER TABLE `team_registrations` DISABLE KEYS */;
INSERT INTO `team_registrations` VALUES (2,1,1,1,'APPROVED','2026-05-27 17:14:43'),(3,1,2,2,'APPROVED','2026-05-27 17:14:43'),(4,2,3,3,'APPROVED','2026-05-27 17:14:43'),(5,2,4,4,'APPROVED','2026-05-27 17:14:43'),(6,3,5,5,'APPROVED','2026-05-27 17:14:43'),(7,1,1,6,'APPROVED','2026-06-01 16:14:08');
/*!40000 ALTER TABLE `team_registrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `teams`
--

LOCK TABLES `teams` WRITE;
/*!40000 ALTER TABLE `teams` DISABLE KEYS */;
INSERT INTO `teams` VALUES (1,'AI Masters',4,'ACTIVE','2026-05-27 17:11:33','AI2026'),(2,'Cyber Legends',5,'ACTIVE','2026-05-27 17:11:33','CYBER26'),(3,'IoT Warriors',18,'ACTIVE','2026-05-27 17:11:33','IOT2026'),(4,'Cloud Ninjas',23,'ACTIVE','2026-05-27 17:11:33','CLOUD26'),(5,'Game Changers',29,'ACTIVE','2026-05-27 17:11:33','GAME2026'),(6,'Mamixi',10034,'ACTIVE','2026-06-01 16:12:42','05162642');
/*!40000 ALTER TABLE `teams` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `universities`
--

LOCK TABLES `universities` WRITE;
/*!40000 ALTER TABLE `universities` DISABLE KEYS */;
INSERT INTO `universities` VALUES (15,'Can Tho University'),(8,'Foreign Trade University'),(1,'FPT University'),(3,'HCM University of Technology'),(5,'HCM University of Technology and Education'),(13,'Hoa Sen University'),(12,'HUTECH University'),(7,'International University - VNUHCM'),(9,'RMIT Vietnam'),(14,'Saigon University'),(10,'Ton Duc Thang University'),(4,'University of Economics Ho Chi Minh City'),(2,'University of Information Technology - VNUHCM'),(6,'University of Science - VNUHCM'),(11,'Van Lang University');
/*!40000 ALTER TABLE `universities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (12,'Admin Coordinator','admin@hackathon.com','$2a$10$adminhashedpassword','COORDINATOR','APPROVED','2026-05-19 17:16:36'),(13,'Nguyen Van Mentor','mentor@hackathon.com','$2a$10$mentorhashedpassword','MENTOR','APPROVED','2026-05-19 17:16:36'),(14,'Tran Thi Judge','judge@hackathon.com','$2a$10$judgehashedpassword','JUDGE_INTERNAL','APPROVED','2026-05-19 17:16:36'),(15,'Le Van Student','student1@fpt.edu.vn','$2a$10$studenthashedpassword','STUDENT_FPT','APPROVED','2026-05-19 17:16:36'),(16,'Pham Thi External','student2@gmail.com','$2a$10$externalhashedpassword','STUDENT_EXTERNAL','APPROVED','2026-05-19 17:16:36'),(28,'thinh','gay@hackathon.com','$2a$10$wouy.f0DVwzpJIW2sZLxNOn6ZnAC/CM/eFLNpouZiwY1qLti3GqQy','STUDENT_FPT','APPROVED','2026-05-19 18:58:07'),(29,'An Nguyen','an12355@gmail.com','$2a$10$xJKHGHxX7yjqWK7hbFBDyueYQDEVwm2T.8G14Cuz6uUXyKmUvDeBy','STUDENT_EXTERNAL','APPROVED','2026-05-20 16:45:42'),(31,'Đặng Cao Bồ','GHP1991@gmail.com','$2a$10$LCQE.hql76IGEzSUWRz6XewyxUAuWfIK581v02HuDFzKh4mLyeMvK','STUDENT_EXTERNAL','APPROVED','2026-05-21 10:58:14'),(39,'Dat Truong','JasonTruong3005@gmail.com','$2a$10$U9ySNxKVvqRtIgDnnF1C/eCNz8eEvx7UH6vH13YjcklcUUsMXCkMq','STUDENT_FPT','APPROVED','2026-05-22 00:03:29'),(41,'Staff1','staff001@gmail.com','$2a$10$Kfo9Zwndn8lY1Q/bZMpYduaPp.cnkqWA..Cdc.furON3tYl71lQiq','COORDINATOR','APPROVED','2026-05-22 09:16:07'),(42,'Staff2','staff002@gmail.com','$2a$10$8Awvxm.jJBS7qj7bwz6UKO2LZqmblN/rpGAPe9JNNf30EjJ7IzjFm','COORDINATOR','APPROVED','2026-05-22 09:17:05'),(43,'Mentor001','mentor001@gmail.com','$2a$10$LwrvsEsSjPUXWxFSvOlon..sro0wlNdt3dDCIG3hpb4OyZWsZ6xXe','MENTOR','APPROVED','2026-05-22 09:18:18'),(44,'Mentor002','mentor002@gmail.com','$2a$10$XyhTFT0SNS6S.naBYj32BOVrjw7UfBdwfbSKoJiD7U3h0hbC92Q4m','MENTOR','APPROVED','2026-05-22 09:18:44'),(45,'Judge001','judge001@gmail.com','$2a$10$XDP3dU1MlcHeC1rM6kpmbu2Ji8qysVXNnf13qxFX.gCb2KcKS66xy','JUDGE_INTERNAL','APPROVED','2026-05-22 09:19:41'),(46,'Judge002','judge002@gmail.com','$2a$10$AgBoPSP70eDcutu2ebcAwO0FRoGcgYhQeE1gOWsYOcQbCOsynpLOC','JUDGE_INTERNAL','APPROVED','2026-05-22 09:20:35'),(47,'Thuan','nmthuan0321@gmail.com','$2a$10$Xsp2gP1Yapv671oMgKs6guu4DGvLevUbo6Ohge/Uo2j5nLF3yTN7q','STUDENT_FPT','APPROVED','2026-05-22 19:25:34'),(10034,'Nguyen Quoc An','an0908252198@gmail.com','$2a$10$9nGbt0hLeYaUlqLj4pDjb.eHkuMvxA3OSV4odUEXlCyHPgCoAUkvW','STUDENT_FPT','APPROVED','2026-05-25 23:32:29');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-03 16:00:31
