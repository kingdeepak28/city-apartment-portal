-- Correction to V3: the society name is "City Apartments" (plural), matching the production
-- domain (city-apartments.in). Same reasoning as V3 - update rather than edit an applied
-- migration in place.
UPDATE settings SET value = 'City Apartments' WHERE key = 'society.name';
