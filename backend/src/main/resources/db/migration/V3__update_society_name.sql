-- V2's seed data used a placeholder society name. Update it rather than editing V2 in place -
-- V2 has already been applied (and checksummed by Flyway) on every existing database, so editing
-- its content directly would fail Flyway's checksum validation on next startup there.
UPDATE settings SET value = 'City Apartment' WHERE key = 'society.name';
