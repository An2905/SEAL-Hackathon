-- Consolidated cleanup migration for unused features removed on 2026-07-31.
-- Every statement is idempotent, so it is safe to run once on existing environments.
-- Drop child tables before their parents to respect foreign-key constraints.

-- Unused criteria-template feature.
DROP TABLE IF EXISTS `criteria_template_items`;
DROP TABLE IF EXISTS `criteria_templates`;

-- Removed chat and WebSocket feature.
DROP TABLE IF EXISTS `chat_messages`;
DROP TABLE IF EXISTS `chat_room_members`;
DROP TABLE IF EXISTS `chat_rooms`;

-- Other unused data features.
DROP TABLE IF EXISTS `announcements`;
DROP TABLE IF EXISTS `audit_logs`;
