-- Destructive cleanup: run only after every backend instance has removed
-- App/AppTimeLimit and clients have switched away from /me/apps.
-- Do not run together with the pre-deployment table creation step.
-- Existing app-specific settings are permanently discarded, not converted.
DROP TABLE IF EXISTS app_time_limits;
DROP TABLE IF EXISTS apps;
