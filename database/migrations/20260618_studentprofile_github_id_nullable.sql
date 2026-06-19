-- Cho phép github_id nhận NULL (chưa liên kết OAuth / xóa liên kết).
-- An toàn chạy lại: MODIFY sang NULL không đổi dữ liệu hiện có.

ALTER TABLE `studentprofile`
  MODIFY COLUMN `github_id` BIGINT NULL DEFAULT NULL;
