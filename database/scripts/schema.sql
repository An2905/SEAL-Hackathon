USE [master]
GO
/****** Object:  Database [Hackathon]    Script Date: 5/27/2026 12:43:15 AM ******/
CREATE DATABASE [Hackathon]
GO
ALTER DATABASE [Hackathon] SET COMPATIBILITY_LEVEL = 150
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [Hackathon].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [Hackathon] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [Hackathon] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [Hackathon] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [Hackathon] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [Hackathon] SET ARITHABORT OFF 
GO
ALTER DATABASE [Hackathon] SET AUTO_CLOSE OFF 
GO
ALTER DATABASE [Hackathon] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [Hackathon] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [Hackathon] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [Hackathon] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [Hackathon] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [Hackathon] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [Hackathon] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [Hackathon] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [Hackathon] SET  ENABLE_BROKER 
GO
ALTER DATABASE [Hackathon] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [Hackathon] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [Hackathon] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [Hackathon] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [Hackathon] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [Hackathon] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [Hackathon] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [Hackathon] SET RECOVERY FULL 
GO
ALTER DATABASE [Hackathon] SET  MULTI_USER 
GO
ALTER DATABASE [Hackathon] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [Hackathon] SET DB_CHAINING OFF 
GO
ALTER DATABASE [Hackathon] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [Hackathon] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [Hackathon] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [Hackathon] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
EXEC sys.sp_db_vardecimal_storage_format N'Hackathon', N'ON'
GO
ALTER DATABASE [Hackathon] SET QUERY_STORE = OFF
GO
USE [Hackathon]
GO
/****** Object:  Table [dbo].[advancement_rules]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[advancement_rules](
	[rule_id] [bigint] IDENTITY(1,1) NOT NULL,
	[round_id] [bigint] NOT NULL,
	[category_id] [bigint] NOT NULL,
	[top_n] [int] NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[rule_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[announcements]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[announcements](
	[announcement_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[title] [varchar](200) NOT NULL,
	[content] [nvarchar](max) NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[announcement_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[audit_logs]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[audit_logs](
	[log_id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[action] [varchar](100) NOT NULL,
	[entity_type] [varchar](100) NULL,
	[entity_id] [bigint] NULL,
	[description] [nvarchar](max) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[log_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[awards]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[awards](
	[award_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[team_id] [bigint] NOT NULL,
	[title] [varchar](100) NOT NULL,
	[rank] [int] NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[award_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[calibration_rounds]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[calibration_rounds](
	[calibration_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[name] [varchar](100) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[calibration_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[calibration_scores]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[calibration_scores](
	[calibration_score_id] [bigint] IDENTITY(1,1) NOT NULL,
	[calibration_id] [bigint] NOT NULL,
	[judge_id] [bigint] NOT NULL,
	[criteria_id] [bigint] NOT NULL,
	[score] [decimal](5, 2) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[calibration_score_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[categories]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[categories](
	[category_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[name] [varchar](100) NOT NULL,
	[description] [nvarchar](max) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[category_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[category_mentors]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[category_mentors](
	[category_id] [bigint] NOT NULL,
	[mentor_id] [bigint] NOT NULL,
	[assigned_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[category_id] ASC,
	[mentor_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[criteria_template_items]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[criteria_template_items](
	[item_id] [bigint] IDENTITY(1,1) NOT NULL,
	[template_id] [bigint] NOT NULL,
	[criterion_name] [varchar](100) NOT NULL,
	[weight] [decimal](5, 2) NOT NULL,
	[max_score] [decimal](5, 2) NOT NULL,
	[description] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[item_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[criteria_templates]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[criteria_templates](
	[template_id] [bigint] IDENTITY(1,1) NOT NULL,
	[name] [varchar](100) NOT NULL,
	[description] [nvarchar](max) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[template_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[eliminations]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[eliminations](
	[elimination_id] [bigint] IDENTITY(1,1) NOT NULL,
	[submission_id] [bigint] NOT NULL,
	[reason] [nvarchar](max) NOT NULL,
	[eliminated_by] [bigint] NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[elimination_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[event_criteria]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[event_criteria](
	[criteria_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[criterion_name] [varchar](100) NOT NULL,
	[weight] [decimal](5, 2) NOT NULL,
	[max_score] [decimal](5, 2) NOT NULL,
	[description] [nvarchar](max) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[criteria_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[events]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[events](
	[event_id] [bigint] IDENTITY(1,1) NOT NULL,
	[title] [varchar](200) NOT NULL,
	[description] [nvarchar](max) NULL,
	[start_date] [datetime] NULL,
	[end_date] [datetime] NULL,
	[status] [nvarchar](255) NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[event_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[judge_assignments]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[judge_assignments](
	[assignment_id] [bigint] IDENTITY(1,1) NOT NULL,
	[judge_id] [bigint] NOT NULL,
	[round_id] [bigint] NOT NULL,
	[category_id] [bigint] NOT NULL,
	[assigned_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[assignment_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[rounds]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[rounds](
	[round_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[name] [varchar](100) NOT NULL,
	[round_order] [int] NOT NULL,
	[submission_deadline] [datetime] NULL,
	[start_date] [datetime] NULL,
	[end_date] [datetime] NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[round_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[score_details]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[score_details](
	[detail_id] [bigint] IDENTITY(1,1) NOT NULL,
	[score_id] [bigint] NOT NULL,
	[criteria_id] [bigint] NOT NULL,
	[score] [decimal](5, 2) NOT NULL,
	[feedback] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[detail_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[scores]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[scores](
	[score_id] [bigint] IDENTITY(1,1) NOT NULL,
	[submission_id] [bigint] NOT NULL,
	[judge_id] [bigint] NOT NULL,
	[total_score] [decimal](6, 2) NULL,
	[submitted_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[score_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[studentProfile]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[studentProfile](
	[profile_id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_id] [bigint] NOT NULL,
	[student_code] [varchar](30) NULL,
	[university_name] [varchar](150) NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[profile_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[submissions]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[submissions](
	[submission_id] [bigint] IDENTITY(1,1) NOT NULL,
	[team_id] [bigint] NOT NULL,
	[round_id] [bigint] NOT NULL,
	[github_url] [nvarchar](max) NULL,
	[demo_url] [nvarchar](max) NULL,
	[report_url] [nvarchar](max) NULL,
	[slide_url] [nvarchar](max) NULL,
	[repository_metadata] [nvarchar](max) NULL,
	[status] [nvarchar](255) NOT NULL,
	[submitted_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[submission_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[team_members]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[team_members](
	[team_id] [bigint] NOT NULL,
	[user_id] [bigint] NOT NULL,
	[joined_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[team_id] ASC,
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[team_registrations]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[team_registrations](
	[registration_id] [bigint] IDENTITY(1,1) NOT NULL,
	[event_id] [bigint] NOT NULL,
	[category_id] [bigint] NOT NULL,
	[team_id] [bigint] NOT NULL,
	[status] [nvarchar](255) NOT NULL,
	[registered_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[registration_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[teams]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[teams](
	[team_id] [bigint] IDENTITY(1,1) NOT NULL,
	[team_name] [varchar](100) NOT NULL,
	[leader_id] [bigint] NOT NULL,
	[status] [nvarchar](255) NOT NULL,
	[created_at] [datetime] NULL,
	[enrollCode] [varchar](50) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[team_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[universities]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[universities](
	[university_id] [bigint] IDENTITY(1,1) NOT NULL,
	[university_name] [nvarchar](255) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[university_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[users]    Script Date: 5/27/2026 12:43:16 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[users](
	[user_id] [bigint] IDENTITY(1,1) NOT NULL,
	[full_name] [nvarchar](100) NULL,
	[email] [varchar](150) NULL,
	[password_hash] [nvarchar](max) NULL,
	[role] [nvarchar](255) NOT NULL,
	[status] [nvarchar](255) NOT NULL,
	[created_at] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[user_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__teams__29E35E0CD2886104]    Script Date: 5/27/2026 12:43:16 AM ******/
ALTER TABLE [dbo].[teams] ADD UNIQUE NONCLUSTERED 
(
	[team_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [UQ__universi__0BE74AEDA18FAC23]    Script Date: 5/27/2026 12:43:16 AM ******/
ALTER TABLE [dbo].[universities] ADD UNIQUE NONCLUSTERED 
(
	[university_name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[advancement_rules] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[announcements] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[audit_logs] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[awards] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[calibration_rounds] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[calibration_scores] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[categories] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[category_mentors] ADD  DEFAULT (getdate()) FOR [assigned_at]
GO
ALTER TABLE [dbo].[criteria_templates] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[eliminations] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[event_criteria] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[events] ADD  DEFAULT ('UPCOMING') FOR [status]
GO
ALTER TABLE [dbo].[events] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[judge_assignments] ADD  DEFAULT (getdate()) FOR [assigned_at]
GO
ALTER TABLE [dbo].[rounds] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[scores] ADD  DEFAULT (getdate()) FOR [submitted_at]
GO
ALTER TABLE [dbo].[studentProfile] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[submissions] ADD  DEFAULT ('SUBMITTED') FOR [status]
GO
ALTER TABLE [dbo].[submissions] ADD  DEFAULT (getdate()) FOR [submitted_at]
GO
ALTER TABLE [dbo].[team_members] ADD  DEFAULT (getdate()) FOR [joined_at]
GO
ALTER TABLE [dbo].[team_registrations] ADD  DEFAULT ('PENDING') FOR [status]
GO
ALTER TABLE [dbo].[team_registrations] ADD  DEFAULT (getdate()) FOR [registered_at]
GO
ALTER TABLE [dbo].[teams] ADD  DEFAULT ('ACTIVE') FOR [status]
GO
ALTER TABLE [dbo].[teams] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[teams] ADD  DEFAULT ('TEMP_CODE') FOR [enrollCode]
GO
ALTER TABLE [dbo].[users] ADD  DEFAULT (getdate()) FOR [created_at]
GO
ALTER TABLE [dbo].[advancement_rules]  WITH CHECK ADD FOREIGN KEY([category_id])
REFERENCES [dbo].[categories] ([category_id])
GO
ALTER TABLE [dbo].[advancement_rules]  WITH CHECK ADD FOREIGN KEY([round_id])
REFERENCES [dbo].[rounds] ([round_id])
GO
ALTER TABLE [dbo].[announcements]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[audit_logs]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[awards]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[awards]  WITH CHECK ADD FOREIGN KEY([team_id])
REFERENCES [dbo].[teams] ([team_id])
GO
ALTER TABLE [dbo].[calibration_rounds]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[calibration_scores]  WITH CHECK ADD FOREIGN KEY([calibration_id])
REFERENCES [dbo].[calibration_rounds] ([calibration_id])
GO
ALTER TABLE [dbo].[calibration_scores]  WITH CHECK ADD FOREIGN KEY([criteria_id])
REFERENCES [dbo].[event_criteria] ([criteria_id])
GO
ALTER TABLE [dbo].[calibration_scores]  WITH CHECK ADD FOREIGN KEY([judge_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[categories]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[category_mentors]  WITH CHECK ADD FOREIGN KEY([category_id])
REFERENCES [dbo].[categories] ([category_id])
GO
ALTER TABLE [dbo].[category_mentors]  WITH CHECK ADD FOREIGN KEY([mentor_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[criteria_template_items]  WITH CHECK ADD FOREIGN KEY([template_id])
REFERENCES [dbo].[criteria_templates] ([template_id])
GO
ALTER TABLE [dbo].[eliminations]  WITH CHECK ADD FOREIGN KEY([eliminated_by])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[eliminations]  WITH CHECK ADD FOREIGN KEY([submission_id])
REFERENCES [dbo].[submissions] ([submission_id])
GO
ALTER TABLE [dbo].[event_criteria]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[judge_assignments]  WITH CHECK ADD FOREIGN KEY([category_id])
REFERENCES [dbo].[categories] ([category_id])
GO
ALTER TABLE [dbo].[judge_assignments]  WITH CHECK ADD FOREIGN KEY([judge_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[judge_assignments]  WITH CHECK ADD FOREIGN KEY([round_id])
REFERENCES [dbo].[rounds] ([round_id])
GO
ALTER TABLE [dbo].[rounds]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[score_details]  WITH CHECK ADD FOREIGN KEY([criteria_id])
REFERENCES [dbo].[event_criteria] ([criteria_id])
GO
ALTER TABLE [dbo].[score_details]  WITH CHECK ADD FOREIGN KEY([score_id])
REFERENCES [dbo].[scores] ([score_id])
GO
ALTER TABLE [dbo].[scores]  WITH CHECK ADD FOREIGN KEY([judge_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[scores]  WITH CHECK ADD FOREIGN KEY([submission_id])
REFERENCES [dbo].[submissions] ([submission_id])
GO
ALTER TABLE [dbo].[studentProfile]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[submissions]  WITH CHECK ADD FOREIGN KEY([round_id])
REFERENCES [dbo].[rounds] ([round_id])
GO
ALTER TABLE [dbo].[submissions]  WITH CHECK ADD FOREIGN KEY([team_id])
REFERENCES [dbo].[teams] ([team_id])
GO
ALTER TABLE [dbo].[team_members]  WITH CHECK ADD FOREIGN KEY([team_id])
REFERENCES [dbo].[teams] ([team_id])
GO
ALTER TABLE [dbo].[team_members]  WITH CHECK ADD FOREIGN KEY([user_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[team_registrations]  WITH CHECK ADD FOREIGN KEY([category_id])
REFERENCES [dbo].[categories] ([category_id])
GO
ALTER TABLE [dbo].[team_registrations]  WITH CHECK ADD FOREIGN KEY([event_id])
REFERENCES [dbo].[events] ([event_id])
GO
ALTER TABLE [dbo].[team_registrations]  WITH CHECK ADD FOREIGN KEY([team_id])
REFERENCES [dbo].[teams] ([team_id])
GO
ALTER TABLE [dbo].[teams]  WITH CHECK ADD FOREIGN KEY([leader_id])
REFERENCES [dbo].[users] ([user_id])
GO
ALTER TABLE [dbo].[events]  WITH CHECK ADD CHECK  (([status]='COMPLETED' OR [status]='ONGOING' OR [status]='UPCOMING'))
GO
ALTER TABLE [dbo].[submissions]  WITH CHECK ADD CHECK  (([status]='DISQUALIFIED' OR [status]='LATE' OR [status]='SUBMITTED'))
GO
ALTER TABLE [dbo].[team_registrations]  WITH CHECK ADD CHECK  (([status]='REJECTED' OR [status]='APPROVED' OR [status]='PENDING'))
GO
ALTER TABLE [dbo].[teams]  WITH CHECK ADD CHECK  (([status]='WITHDRAWN' OR [status]='DISQUALIFIED' OR [status]='ELIMINATED' OR [status]='ACTIVE'))
GO
ALTER TABLE [dbo].[users]  WITH CHECK ADD CHECK  (([status]='REJECTED' OR [status]='APPROVED' OR [status]='PENDING'))
GO
USE [master]
GO
ALTER DATABASE [Hackathon] SET  READ_WRITE 
GO
