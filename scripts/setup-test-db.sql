-- =====================================================================
--  Sartia - throwaway database for the integration tests
--
--  The failsafe configuration in pom.xml points RentalLifecycleIT and
--  ConcurrentRentalIT at `sartia_test` (via -Ddb.url) instead of the
--  application's `sartia` database, so a test run can never touch the real
--  catalogue. Run this ONCE to create that schema:
--
--      mysql -u root -p < scripts/setup-test-db.sql
--
--  It is re-runnable (drops and recreates the tables). The table
--  definitions mirror db/schema.sql - keep them in step if the schema
--  changes. Only the category rows are seeded; the tests create every
--  film, copy, customer and rental they need and clean them up afterwards.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS sartia_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sartia_test;

DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS rentals;
DROP TABLE IF EXISTS copies;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  username       VARCHAR(50)  NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  email          VARCHAR(255) NOT NULL,
  full_name      VARCHAR(100) NOT NULL,
  phone          VARCHAR(30)           DEFAULT NULL,
  role           ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
  active         BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email    (email)
) ENGINE = InnoDB;

CREATE TABLE categories (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50) NOT NULL,
  description VARCHAR(255)         DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_name (name)
) ENGINE = InnoDB;

CREATE TABLE movies (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  title        VARCHAR(200) NOT NULL,
  description  TEXT                  DEFAULT NULL,
  director     VARCHAR(100)          DEFAULT NULL,
  release_year SMALLINT              DEFAULT NULL,
  duration_min SMALLINT              DEFAULT NULL,
  category_id  BIGINT       NOT NULL,
  cover_url    VARCHAR(500)          DEFAULT NULL,
  daily_price  DECIMAL(6,2) NOT NULL DEFAULT 5.00,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_movies_category
    FOREIGN KEY (category_id) REFERENCES categories (id),
  KEY idx_movies_category (category_id),
  KEY idx_movies_title    (title)
) ENGINE = InnoDB;

CREATE TABLE copies (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  movie_id    BIGINT      NOT NULL,
  barcode     VARCHAR(40) NOT NULL,
  status      ENUM('AVAILABLE','RENTED','LOST') NOT NULL DEFAULT 'AVAILABLE',
  acquired_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_copies_barcode (barcode),
  CONSTRAINT fk_copies_movie
    FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
  KEY idx_copies_movie_status (movie_id, status)
) ENGINE = InnoDB;

CREATE TABLE rentals (
  id          BIGINT    NOT NULL AUTO_INCREMENT,
  copy_id     BIGINT    NOT NULL,
  user_id     BIGINT    NOT NULL,
  rented_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_date    DATE      NOT NULL,
  returned_at TIMESTAMP          DEFAULT NULL,
  late_fee    DECIMAL(6,2)       DEFAULT NULL,
  -- Uniqueness discriminator: open rental -> 1, returned -> NULL. With the
  -- unique key below this makes a second OPEN rental of a copy impossible at
  -- the storage layer, which is exactly what ConcurrentRentalIT verifies.
  active_flag TINYINT GENERATED ALWAYS AS (CASE WHEN returned_at IS NULL THEN 1 END) STORED,
  PRIMARY KEY (id),
  CONSTRAINT fk_rentals_copy FOREIGN KEY (copy_id) REFERENCES copies (id),
  CONSTRAINT fk_rentals_user FOREIGN KEY (user_id) REFERENCES users (id),
  UNIQUE KEY uq_rentals_copy_active (copy_id, active_flag),
  KEY idx_rentals_user (user_id, rented_at),
  KEY idx_rentals_open (returned_at, due_date)
) ENGINE = InnoDB;

CREATE TABLE reviews (
  id         BIGINT    NOT NULL AUTO_INCREMENT,
  movie_id   BIGINT    NOT NULL,
  user_id    BIGINT    NOT NULL,
  rating     TINYINT   NOT NULL,
  comment    TEXT               DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_reviews_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_user  FOREIGN KEY (user_id)  REFERENCES users (id),
  CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  UNIQUE KEY uq_reviews_movie_user (movie_id, user_id),
  KEY idx_reviews_movie (movie_id)
) ENGINE = InnoDB;

-- The tests seed films under category id 1; the fixed genre list must exist
-- so the movies.category_id foreign key is satisfied.
INSERT INTO categories (id, name, description) VALUES
 (1, 'Action',          'Action, thrillers and chases'),
 (2, 'Comedy',          'Light-hearted and funny films'),
 (3, 'Drama',           'Story-driven and emotional films'),
 (4, 'Science Fiction', 'The future, space and technology'),
 (5, 'Animation',       'Animated films for the whole family'),
 (6, 'Horror',          'Suspense and horror'),
 (7, 'Documentary',     'Documentary films');
