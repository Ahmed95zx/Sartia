-- =====================================================================
--  Sartia - Video/DVD Rental System
--  Database schema (MySQL 8.0+, InnoDB)
--
--  Design notes
--  ------------
--  * A MOVIE is a catalogue title. A COPY is one physical disc of that
--    title. Rentals are made against a COPY, never against a MOVIE.
--    This is what makes stock tracking correct: two customers may rent
--    the same title concurrently if, and only if, two copies exist.
--
--  * Concurrency is enforced by the database itself, not only by the
--    application, via the generated column `active_flag` on `rentals`
--    (see the comment there). Application-level locking alone would be
--    unsafe once more than one server process is involved.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS sartia
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sartia;

-- Dropped in reverse dependency order so the script is re-runnable.
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS rentals;
DROP TABLE IF EXISTS copies;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;


-- ---------------------------------------------------------------------
-- users - customers and administrators.
-- ---------------------------------------------------------------------
CREATE TABLE users (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  username       VARCHAR(50)  NOT NULL,
  -- PBKDF2-HMAC-SHA256, stored as "iterations:salt:hash" (all Base64).
  -- Never a plaintext or unsalted-digest password.
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


-- ---------------------------------------------------------------------
-- categories - the fixed genre list the catalogue is organised by.
-- ---------------------------------------------------------------------
CREATE TABLE categories (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50) NOT NULL,
  description VARCHAR(255)         DEFAULT NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_name (name)
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- movies - catalogue titles.
-- ---------------------------------------------------------------------
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

  -- Deliberately no FULLTEXT index. MySQL's full-text matching works on whole
  -- words or their prefixes, so it cannot match a fragment from the middle of
  -- a word: 'batman' would not find "The Dark Knight". MovieDao uses substring
  -- matching instead; see the note on that class. The original Hebrew
  -- catalogue made the case stronger still, because Hebrew attaches its
  -- definite article to the front of a word (המטריקס = ה + מטריקס), so a
  -- prefix search could never match the form a customer actually types.
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- copies - physical inventory. One row per rentable disc.
-- ---------------------------------------------------------------------
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

  -- Composite index ordered (movie_id, status): the availability lookup
  -- filters on both and is the hottest query in the system.
  KEY idx_copies_movie_status (movie_id, status)
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- rentals - one row per checkout. An OPEN rental has returned_at IS NULL.
-- ---------------------------------------------------------------------
CREATE TABLE rentals (
  id          BIGINT    NOT NULL AUTO_INCREMENT,
  copy_id     BIGINT    NOT NULL,
  user_id     BIGINT    NOT NULL,
  rented_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_date    DATE      NOT NULL,
  returned_at TIMESTAMP          DEFAULT NULL,
  late_fee    DECIMAL(6,2)       DEFAULT NULL,

  -- Generated column used purely as a uniqueness discriminator:
  --   open rental     -> 1
  --   returned rental -> NULL
  -- MySQL's unique indexes ignore NULLs, so (copy_id, active_flag) permits
  -- unlimited *returned* rentals of a copy but at most ONE open rental.
  -- This makes double-renting the same disc impossible at the storage
  -- layer even if two application servers race.
  active_flag TINYINT GENERATED ALWAYS AS (CASE WHEN returned_at IS NULL THEN 1 END) STORED,

  PRIMARY KEY (id),
  CONSTRAINT fk_rentals_copy
    FOREIGN KEY (copy_id) REFERENCES copies (id),
  CONSTRAINT fk_rentals_user
    FOREIGN KEY (user_id) REFERENCES users (id),

  UNIQUE KEY uq_rentals_copy_active (copy_id, active_flag),
  KEY idx_rentals_user      (user_id, rented_at),
  KEY idx_rentals_open      (returned_at, due_date)
) ENGINE = InnoDB;


-- ---------------------------------------------------------------------
-- reviews - optional customer ratings, one per customer per title.
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
  id         BIGINT    NOT NULL AUTO_INCREMENT,
  movie_id   BIGINT    NOT NULL,
  user_id    BIGINT    NOT NULL,
  rating     TINYINT   NOT NULL,
  comment    TEXT               DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  CONSTRAINT fk_reviews_movie
    FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
  CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),

  UNIQUE KEY uq_reviews_movie_user (movie_id, user_id),
  KEY idx_reviews_movie (movie_id)
) ENGINE = InnoDB;
