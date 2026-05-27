USE [Hackathon]
GO

-- Level 0: Foundation Tables (No foreign key dependencies in this script)
SET IDENTITY_INSERT [dbo].[users] ON 
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (1, N'Admin Coordinator', N'admin@hackathon.com', N'123456', N'COORDINATOR', N'APPROVED', CAST(N'2026-05-19T16:05:44.353' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (2, N'Nguyen Van Mentor', N'mentor@hackathon.com', N'123456', N'MENTOR', N'APPROVED', CAST(N'2026-05-19T16:05:44.353' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (3, N'Tran Thi Judge', N'judge@hackathon.com', N'123456', N'JUDGE_INTERNAL', N'APPROVED', CAST(N'2026-05-19T16:05:44.353' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (4, N'Le Van Student', N'student1@fpt.edu.vn', N'123456', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-19T16:05:44.353' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (5, N'Pham Thi External', N'student2@gmail.com', N'123456', N'STUDENT_EXTERNAL', N'APPROVED', CAST(N'2026-05-19T16:05:44.353' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (6, N'Admin Coordinator', N'admin@hackathon.com', N'123456', N'COORDINATOR', N'APPROVED', CAST(N'2026-05-19T16:05:52.240' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (7, N'Nguyen Van Mentor', N'mentor@hackathon.com', N'123456', N'MENTOR', N'APPROVED', CAST(N'2026-05-19T16:05:52.240' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (8, N'Tran Thi Judge', N'judge@hackathon.com', N'123456', N'JUDGE_INTERNAL', N'APPROVED', CAST(N'2026-05-19T16:05:52.240' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (9, N'Le Van Student', N'student1@fpt.edu.vn', N'123456', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-19T16:05:52.240' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (10, N'Pham Thi External', N'student2@gmail.com', N'123456', N'STUDENT_EXTERNAL', N'APPROVED', CAST(N'2026-05-19T16:05:52.240' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (12, N'Admin Coordinator', N'admin@hackathon.com', N'$2a$10$adminhashedpassword', N'COORDINATOR', N'APPROVED', CAST(N'2026-05-19T17:16:36.493' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (13, N'Nguyen Van Mentor', N'mentor@hackathon.com', N'$2a$10$mentorhashedpassword', N'MENTOR', N'APPROVED', CAST(N'2026-05-19T17:16:36.493' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (14, N'Tran Thi Judge', N'judge@hackathon.com', N'$2a$10$judgehashedpassword', N'JUDGE_INTERNAL', N'APPROVED', CAST(N'2026-05-19T17:16:36.493' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (15, N'Le Van Student', N'student1@fpt.edu.vn', N'$2a$10$studenthashedpassword', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-19T17:16:36.493' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (16, N'Pham Thi External', N'student2@gmail.com', N'$2a$10$externalhashedpassword', N'STUDENT_EXTERNAL', N'APPROVED', CAST(N'2026-05-19T17:16:36.493' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (18, N'Huy', N'Huy@hackathon.com', N'123456', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-19T17:57:00.180' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (23, N'An Nguyen', N'An@hackathon.com', N'123', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-19T18:19:18.653' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (28, N'thinh', N'gay@hackathon.com', N'$2a$10$wouy.f0DVwzpJIW2sZLxNOn6ZnAC/CM/eFLNpouZiwY1qLti3GqQy', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-19T18:58:07.540' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (29, N'An Nguyen', N'an12355@gmail.com', N'$2a$10$xJKHGHxX7yjqWK7hbFBDyueYQDEVwm2T.8G14Cuz6uUXyKmUvDeBy', N'STUDENT_EXTERNAL', N'APPROVED', CAST(N'2026-05-20T16:45:42.430' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (31, N'Đặng Cao Bồ', N'GHP1991@gmail.com', N'$2a$10$LCQE.hql76IGEzSUWRz6XewyxUAuWfIK581v02HuDFzKh4mLyeMvK', N'STUDENT_EXTERNAL', N'APPROVED', CAST(N'2026-05-21T10:58:14.053' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (39, N'Dat Truong', N'JasonTruong3005@gmail.com', N'$2a$10$U9ySNxKVvqRtIgDnnF1C/eCNz8eEvx7UH6vH13YjcklcUUsMXCkMq', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-22T00:03:29.200' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (41, N'Staff1', N'staff001@gmail.com', N'$2a$10$Kfo9Zwndn8lY1Q/bZMpYduaPp.cnkqWA..Cdc.furON3tYl71lQiq', N'COORDINATOR', N'APPROVED', CAST(N'2026-05-22T09:16:07.900' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (42, N'Staff2', N'staff002@gmail.com', N'$2a$10$8Awvxm.jJBS7qj7bwz6UKO2LZqmblN/rpGAPe9JNNf30EjJ7IzjFm', N'COORDINATOR', N'APPROVED', CAST(N'2026-05-22T09:17:05.713' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (43, N'Mentor001', N'mentor001@gmail.com', N'$2a$10$LwrvsEsSjPUXWxFSvOlon..sro0wlNdt3dDCIG3hpb4OyZWsZ6xXe', N'MENTOR', N'APPROVED', CAST(N'2026-05-22T09:18:18.207' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (44, N'Mentor002', N'mentor002@gmail.com', N'$2a$10$XyhTFT0SNS6S.naBYj32BOVrjw7UfBdwfbSKoJiD7U3h0hbC92Q4m', N'MENTOR', N'APPROVED', CAST(N'2026-05-22T09:18:44.413' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (45, N'Judge001', N'judge001@gmail.com', N'$2a$10$XDP3dU1MlcHeC1rM6kpmbu2Ji8qysVXNnf13qxFX.gCb2KcKS66xy', N'JUDGE_INTERNAL', N'APPROVED', CAST(N'2026-05-22T09:19:41.067' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (46, N'Judge002', N'judge002@gmail.com', N'$2a$10$AgBoPSP70eDcutu2ebcAwO0FRoGcgYhQeE1gOWsYOcQbCOsynpLOC', N'JUDGE_INTERNAL', N'APPROVED', CAST(N'2026-05-22T09:20:35.837' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (47, N'Thuan', N'nmthuan0321@gmail.com', N'$2a$10$Xsp2gP1Yapv671oMgKs6guu4DGvLevUbo6Ohge/Uo2j5nLF3yTN7q', N'STUDENT_FPT', N'APPROVED', CAST(N'2026-05-22T19:25:34.230' AS DateTime))
INSERT [dbo].[users] ([user_id], [full_name], [email], [password_hash], [role], [status], [created_at]) VALUES (10034, N'Nguyen Quoc An', N'an0908252198@gmail.com', N'$2a$10$CikqeSGzhK1ii2DnTF73IeWuU99WyQmxhibAwaNJkajpRjEBYI0jC', N'MENTOR', N'APPROVED', CAST(N'2026-05-25T23:32:29.220' AS DateTime))
SET IDENTITY_INSERT [dbo].[users] OFF
GO

SET IDENTITY_INSERT [dbo].[events] ON 
INSERT [dbo].[events] ([event_id], [title], [description], [start_date], [end_date], [status], [created_at]) VALUES (1, N'FPT AI Hackathon 2026', N'AI innovation competition', CAST(N'2026-06-01T00:00:00.000' AS DateTime), CAST(N'2026-06-30T00:00:00.000' AS DateTime), N'UPCOMING', CAST(N'2026-05-19T16:05:44.373' AS DateTime))
INSERT [dbo].[events] ([event_id], [title], [description], [start_date], [end_date], [status], [created_at]) VALUES (2, N'FPT Sience', N'AI innovation competition', CAST(N'2026-06-01T00:00:00.000' AS DateTime), CAST(N'2026-06-30T00:00:00.000' AS DateTime), N'UPCOMING', CAST(N'2026-05-19T16:05:52.247' AS DateTime))
INSERT [dbo].[events] ([event_id], [title], [description], [start_date], [end_date], [status], [created_at]) VALUES (3, N'FPT Tech', N'AI innovation competition', CAST(N'2026-06-01T00:00:00.000' AS DateTime), CAST(N'2026-06-30T00:00:00.000' AS DateTime), N'UPCOMING', CAST(N'2026-05-19T17:16:36.510' AS DateTime))
SET IDENTITY_INSERT [dbo].[events] OFF
GO

SET IDENTITY_INSERT [dbo].[universities] ON 
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (15, N'Can Tho University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (8, N'Foreign Trade University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (1, N'FPT University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (3, N'HCM University of Technology')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (5, N'HCM University of Technology and Education')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (13, N'Hoa Sen University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (12, N'HUTECH University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (7, N'International University - VNUHCM')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (9, N'RMIT Vietnam')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (14, N'Saigon University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (10, N'Ton Duc Thang University')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (4, N'University of Economics Ho Chi Minh City')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (2, N'University of Information Technology - VNUHCM')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (6, N'University of Science - VNUHCM')
INSERT [dbo].[universities] ([university_id], [university_name]) VALUES (11, N'Van Lang University')
SET IDENTITY_INSERT [dbo].[universities] OFF
GO

-- Level 1: Tables depending on Level 0
SET IDENTITY_INSERT [dbo].[studentProfile] ON 
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (1, 4, N'SE182000', N'FPT University', CAST(N'2026-05-19T17:14:49.307' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (2, 5, N'SE182001', N'HCM University of Technology', CAST(N'2026-05-19T17:14:49.307' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (3, 4, N'SE182000', N'FPT University', CAST(N'2026-05-19T17:16:36.503' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (4, 5, N'EXT2026', N'HCM University of Technology', CAST(N'2026-05-19T17:16:36.503' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (6, 18, N'HE199999', N'FPT University', CAST(N'2026-05-19T17:57:15.027' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (7, 23, N'SE123456', N'FPT HCM', CAST(N'2026-05-19T18:19:18.673' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (12, 28, N'FE112233', N'FPTU', CAST(N'2026-05-19T18:58:07.550' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (15, 31, N'BK102304', N'BKU', CAST(N'2026-05-21T10:58:14.060' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (17, 29, N'SE192626', N'FPT', CAST(N'2026-05-21T21:13:44.257' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (24, 39, N'SE205201', N'FPTU', CAST(N'2026-05-22T00:03:29.217' AS DateTime))
INSERT [dbo].[studentProfile] ([profile_id], [user_id], [student_code], [university_name], [created_at]) VALUES (25, 47, N'SE123123', N'FPTU', CAST(N'2026-05-22T19:25:34.250' AS DateTime))
SET IDENTITY_INSERT [dbo].[studentProfile] OFF
GO

SET IDENTITY_INSERT [dbo].[categories] ON 
INSERT [dbo].[categories] ([category_id], [event_id], [name], [description], [created_at]) VALUES (1, 1, N'Artificial Intelligence', N'AI solutions and machine learning', CAST(N'2026-05-19T16:05:44.383' AS DateTime))
INSERT [dbo].[categories] ([category_id], [event_id], [name], [description], [created_at]) VALUES (2, 1, N'Web Application', N'Modern web platform solutions', CAST(N'2026-05-19T16:05:44.383' AS DateTime))
INSERT [dbo].[categories] ([category_id], [event_id], [name], [description], [created_at]) VALUES (3, 1, N'Artificial Intelligence', N'AI solutions and machine learning', CAST(N'2026-05-19T16:05:52.250' AS DateTime))
INSERT [dbo].[categories] ([category_id], [event_id], [name], [description], [created_at]) VALUES (4, 1, N'Web Application', N'Modern web platform solutions', CAST(N'2026-05-19T16:05:52.250' AS DateTime))
INSERT [dbo].[categories] ([category_id], [event_id], [name], [description], [created_at]) VALUES (5, 1, N'Artificial Intelligence', N'AI solutions and machine learning', CAST(N'2026-05-19T17:16:36.520' AS DateTime))
INSERT [dbo].[categories] ([category_id], [event_id], [name], [description], [created_at]) VALUES (6, 1, N'Web Application', N'Modern web platform solutions', CAST(N'2026-05-19T17:16:36.520' AS DateTime))
SET IDENTITY_INSERT [dbo].[categories] OFF
GO

SET IDENTITY_INSERT [dbo].[teams] ON 
INSERT [dbo].[teams] ([team_id], [team_name], [leader_id], [status], [created_at], [enrollCode]) VALUES (1, N'Code Warriors', 4, N'ACTIVE', CAST(N'2026-05-19T16:05:44.393' AS DateTime), N'12345678')
INSERT [dbo].[teams] ([team_id], [team_name], [leader_id], [status], [created_at], [enrollCode]) VALUES (8, N'CODERS', 29, N'ACTIVE', CAST(N'2026-05-20T16:49:25.900' AS DateTime), N'70565852')
INSERT [dbo].[teams] ([team_id], [team_name], [leader_id], [status], [created_at], [enrollCode]) VALUES (9, N'DoMixi', 31, N'ACTIVE', CAST(N'2026-05-21T11:08:45.273' AS DateTime), N'36525192')
INSERT [dbo].[teams] ([team_id], [team_name], [leader_id], [status], [created_at], [enrollCode]) VALUES (11, N'mamixi', 47, N'ACTIVE', CAST(N'2026-05-22T19:26:16.937' AS DateTime), N'52776902')
SET IDENTITY_INSERT [dbo].[teams] OFF
GO

-- Level 2: Junction Tables and Detail Tables
INSERT [dbo].[team_members] ([team_id], [user_id], [joined_at]) VALUES (1, 4, CAST(N'2026-05-20T11:23:00.417' AS DateTime))
INSERT [dbo].[team_members] ([team_id], [user_id], [joined_at]) VALUES (1, 28, CAST(N'2026-05-20T12:17:24.247' AS DateTime))
INSERT [dbo].[team_members] ([team_id], [user_id], [joined_at]) VALUES (8, 29, CAST(N'2026-05-20T16:49:25.907' AS DateTime))
INSERT [dbo].[team_members] ([team_id], [user_id], [joined_at]) VALUES (9, 31, CAST(N'2026-05-21T11:08:45.283' AS DateTime))
INSERT [dbo].[team_members] ([team_id], [user_id], [joined_at]) VALUES (11, 47, CAST(N'2026-05-22T19:26:16.940' AS DateTime))
GO

SET IDENTITY_INSERT [dbo].[team_registrations] ON 
INSERT [dbo].[team_registrations] ([registration_id], [event_id], [category_id], [team_id], [status], [registered_at]) VALUES (1, 1, 1, 1, N'APPROVED', CAST(N'2026-05-19T16:05:44.403' AS DateTime))
INSERT [dbo].[team_registrations] ([registration_id], [event_id], [category_id], [team_id], [status], [registered_at]) VALUES (5, 1, 1, 9, N'PENDING', CAST(N'2026-05-21T11:09:04.920' AS DateTime))
SET IDENTITY_INSERT [dbo].[team_registrations] OFF
GO

INSERT [dbo].[category_mentors] ([category_id], [mentor_id], [assigned_at]) VALUES (1, 2, CAST(N'2026-05-19T16:05:44.387' AS DateTime))
GO
