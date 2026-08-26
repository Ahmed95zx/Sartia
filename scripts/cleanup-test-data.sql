-- =====================================================================
--  Sartia - remove integration-test residue from the LIVE catalogue
--
--  Why this exists
--  ---------------
--  The integration tests (RentalLifecycleIT, ConcurrentRentalIT) used to
--  run against this same `sartia` database and COMMIT the films, copies,
--  customers and rentals they seed without ever deleting them. The result
--  is junk catalogue entries such as "admin return 267617122000" and
--  "barcode sequence 267485276399" (all filed under category 1, Action),
--  plus throwaway @test.local users.
--
--  Test rows carry a signature real data never has:
--    * copies:  barcode LIKE 'LC-%' or 'IT-%'   (real copies are 'SRT-%')
--    * users:   email   LIKE '%@test.local'     (real users are not)
--  so this deletes precisely those, in foreign-key-safe order, and leaves
--  every real title untouched.
--
--  How to run
--  ----------
--      mysql -u root -p sartia < scripts/cleanup-test-data.sql
--
--  It runs inside ONE transaction and prints a before/after summary. To
--  preview WITHOUT changing anything, change the final COMMIT (last line)
--  to ROLLBACK and re-run - the summary still shows what would have gone.
-- =====================================================================

USE sartia;

START TRANSACTION;

-- ---- Before: how much test residue is present ----------------------
SELECT 'test films present' AS what,
       COUNT(DISTINCT m.id)  AS count
  FROM movies m JOIN copies c ON c.movie_id = m.id
 WHERE c.barcode LIKE 'LC-%' OR c.barcode LIKE 'IT-%'
UNION ALL
SELECT 'test users present', COUNT(*)
  FROM users WHERE email LIKE '%@test.local';

-- ---- 1. rentals: FK to copies and users, no cascade, so clear first -
DELETE r FROM rentals r JOIN copies c ON c.id = r.copy_id
 WHERE c.barcode LIKE 'LC-%' OR c.barcode LIKE 'IT-%';
DELETE r FROM rentals r JOIN users u ON u.id = r.user_id
 WHERE u.email LIKE '%@test.local';

-- ---- 2. reviews: for a test film, or written by a test user ---------
DELETE rv FROM reviews rv JOIN copies c ON c.movie_id = rv.movie_id
 WHERE c.barcode LIKE 'LC-%' OR c.barcode LIKE 'IT-%';
DELETE rv FROM reviews rv JOIN users u ON u.id = rv.user_id
 WHERE u.email LIKE '%@test.local';

-- ---- 3. movies owning a test copy: cascades their copies + reviews --
DELETE m FROM movies m JOIN copies c ON c.movie_id = m.id
 WHERE c.barcode LIKE 'LC-%' OR c.barcode LIKE 'IT-%';

-- ---- 4. any stray test copies whose film a test already deleted -----
DELETE FROM copies WHERE barcode LIKE 'LC-%' OR barcode LIKE 'IT-%';

-- ---- 5. the throwaway customers, now unreferenced ------------------
DELETE FROM users WHERE email LIKE '%@test.local';

-- ---- After: confirm the residue is gone ----------------------------
SELECT 'films remaining'   AS what, COUNT(*) AS count FROM movies
UNION ALL
SELECT 'users remaining',  COUNT(*) FROM users
UNION ALL
SELECT 'test copies left',
       COUNT(*) FROM copies WHERE barcode LIKE 'LC-%' OR barcode LIKE 'IT-%'
UNION ALL
SELECT 'test users left',
       COUNT(*) FROM users WHERE email LIKE '%@test.local';

-- Change to ROLLBACK to preview instead of apply.
COMMIT;
