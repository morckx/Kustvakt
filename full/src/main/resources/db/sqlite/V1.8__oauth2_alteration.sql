-- ND:2020-04-15
-- Needs to be dropped as null-URLs result in non-unique hash codes
DROP INDEX IF EXISTS client_url_index;