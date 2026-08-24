-- =====================================================================
--  Sartia - demonstration data
--
--  Run after schema.sql. Safe to re-run: it clears the tables it fills.
--
--  The password_hash values below are genuine PBKDF2-HMAC-SHA256 hashes
--  produced by util.Passwords, not placeholders. To mint new ones:
--      java -cp target/classes util.Passwords <password>
--
--  Cover images are stored under src/main/webapp/images/covers and are
--  referenced by an application-relative path, so the catalogue renders
--  with no network connection. cover_url also accepts a full external
--  URL if a title is maintained that way instead.
--
--  Demo accounts
--  -------------
--      admin / admin123   (administrator)
--      david / david123   (customer)
--      noa   / noa123     (customer)
-- =====================================================================

USE sartia;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE reviews;
TRUNCATE TABLE rentals;
TRUNCATE TABLE copies;
TRUNCATE TABLE movies;
TRUNCATE TABLE categories;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;


-- ---------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------
INSERT INTO users (id, username, password_hash, email, full_name, phone, role) VALUES
 (1, 'admin', '210000:t4Yp3elTM1vCsm5U/52Uew==:G57IpiiF6OLC1zW8Eoc6daeaUG+JC218x4ezdqZ+Rek=',
     'admin@sartia.co.il',  'System Administrator', '03-5555555', 'ADMIN'),
 (2, 'david', '210000:IImw9y8E76IwAiEGvpIxMQ==:+4OLVh5dvDOSpIVl5EXUN1uDh4M1NSQZ3w9cZUhq9Sc=',
     'david@example.com',   'David Cohen',          '052-1234567', 'CUSTOMER'),
 (3, 'noa',   '210000:h1HLfjSWTjX0wILDaI1Vmg==:65YQidWEz2T1ay9Hqym2PUc6oB5Oa9tBf2zg9U1cw6A=',
     'noa@example.com',     'Noa Levi',             '054-7654321', 'CUSTOMER');


-- ---------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------
INSERT INTO categories (id, name, description) VALUES
 (1, 'Action',          'Action, thrillers and chases'),
 (2, 'Comedy',          'Light-hearted and funny films'),
 (3, 'Drama',           'Story-driven and emotional films'),
 (4, 'Science Fiction', 'The future, space and technology'),
 (5, 'Animation',       'Animated films for the whole family'),
 (6, 'Horror',          'Suspense and horror'),
 (7, 'Documentary',     'Documentary films');


-- ---------------------------------------------------------------------
-- Movies
-- ---------------------------------------------------------------------
INSERT INTO movies (id, title, description, director, release_year, duration_min, category_id, cover_url, daily_price) VALUES
 (1,  'The Matrix',
      'A hacker discovers that the reality he knows is a simulation, and joins a rebellion against the machines that rule humanity.',
      'Lana and Lilly Wachowski', 1999, 136, 4, '/images/covers/1.jpg', 6.00),
 (2,  'Blade Runner 2049',
      'A young officer uncovers a buried secret that could change what remains of human society.',
      'Denis Villeneuve', 2017, 164, 4, '/images/covers/2.jpg', 7.00),
 (3,  'The Dark Knight',
      'Batman faces the Joker, a chaotic criminal who threatens to bring Gotham City to a standstill.',
      'Christopher Nolan', 2008, 152, 1, '/images/covers/3.jpg', 6.50),
 (4,  'Mad Max: Fury Road',
      'A relentless chase across a post-apocalyptic desert, in a bid to escape a brutal warlord.',
      'George Miller', 2015, 120, 1, '/images/covers/4.jpg', 6.00),
 (5,  'The Imitation Game',
      'Alan Turing leads the team that cracked the Enigma code during the Second World War.',
      'Morten Tyldum', 2014, 114, 3, '/images/covers/5.jpg', 5.50),
 (6,  'Forrest Gump',
      'A simple man crosses decades of American history without ever meaning to.',
      'Robert Zemeckis', 1994, 142, 3, '/images/covers/6.jpg', 5.00),
 (7,  'The Grand Budapest Hotel',
      'A legendary concierge and his protege are caught up in a murder and a disputed inheritance in interwar Europe.',
      'Wes Anderson', 2014, 99, 2, '/images/covers/7.jpg', 5.50),
 (8,  'Superbad',
      'Two friends try to survive the last night of high school.',
      'Greg Mottola', 2007, 113, 2, '/images/covers/8.jpg', 4.50),
 (9,  'Spirited Away',
      'A girl is trapped in a world of spirits and struggles to save her parents and find her way home.',
      'Hayao Miyazaki', 2001, 125, 5, '/images/covers/9.jpg', 5.50),
 (10, 'Monsters, Inc.',
      'Two monsters discover a human child and learn that everything they were taught was wrong.',
      'Pete Docter', 2001, 92, 5, '/images/covers/10.jpg', 5.00),
 (11, 'Catch Me If You Can',
      'A young con artist poses as a pilot, a doctor and a lawyer while an FBI agent closes in.',
      'Steven Spielberg', 2002, 141, 3, '/images/covers/11.jpg', 5.00),
 (12, 'The Ring',
      'A journalist investigates a cursed videotape that kills whoever watches it a week later.',
      'Gore Verbinski', 2002, 115, 6, '/images/covers/12.jpg', 5.50),
 (13, 'The Shawshank Redemption',
      'A banker convicted of a murder he did not commit spends years quietly building his way to freedom.',
      'Frank Darabont', 1994, 142, 3, '/images/covers/13.jpg', 6.00),
 (14, 'Dawn of the Planet of the Apes',
      'Intelligent apes confront the remnants of humanity over control of the Earth.',
      'Matt Reeves', 2014, 130, 4, '/images/covers/14.jpg', 5.50),
 (15, 'Man on Wire',
      'The story of a high-wire artist on his way to walking between the Twin Towers.',
      'James Marsh', 2008, 94, 7, '/images/covers/15.jpg', 4.50),
 (16, 'Die Hard',
      'A New York detective takes on a group of armed thieves who have seized a Los Angeles office tower.',
      'John McTiernan', 1988, 132, 1, '/images/covers/16.jpg', 5.50),
 (17, 'Gladiator',
      'A betrayed Roman general is sold into slavery and fights his way back to confront the emperor who destroyed his family.',
      'Ridley Scott', 2000, 155, 1, '/images/covers/17.jpg', 6.00),
 (18, 'Groundhog Day',
      'A cynical weatherman is trapped reliving the same day until he learns to live it differently.',
      'Harold Ramis', 1993, 101, 2, '/images/covers/18.jpg', 5.00),
 (19, 'The Big Lebowski',
      'A case of mistaken identity drags an easy-going Los Angeles slacker into a kidnapping plot.',
      'Joel and Ethan Coen', 1998, 117, 2, '/images/covers/19.jpg', 5.00),
 (20, 'Interstellar',
      'A team of explorers travels through a wormhole in search of a new home for humanity.',
      'Christopher Nolan', 2014, 169, 4, '/images/covers/20.jpg', 6.50),
 (21, 'Toy Story',
      'A cowboy doll fears replacement when a spaceman toy becomes the new favourite.',
      'John Lasseter', 1995, 81, 5, '/images/covers/21.jpg', 5.00),
 (22, 'Coco',
      'A boy who dreams of music crosses into the Land of the Dead to uncover his family history.',
      'Lee Unkrich', 2017, 105, 5, '/images/covers/22.jpg', 5.50),
 (23, 'The Shining',
      'A caretaker and his family spend a winter alone in an empty hotel, and the isolation begins to work on him.',
      'Stanley Kubrick', 1980, 146, 6, '/images/covers/23.jpg', 5.50),
 (24, 'Get Out',
      'A weekend visit to the family of his girlfriend turns steadily more sinister.',
      'Jordan Peele', 2017, 104, 6, '/images/covers/24.jpg', 5.50),
 (25, 'A Quiet Place',
      'A family lives in silence to hide from creatures that hunt by sound.',
      'John Krasinski', 2018, 90, 6, '/images/covers/25.jpg', 5.50),
 (26, 'March of the Penguins',
      'Emperor penguins cross the Antarctic ice each year to breed in one of the harshest places on Earth.',
      'Luc Jacquet', 2005, 80, 7, '/images/covers/26.jpg', 4.50),
 (27, 'Free Solo',
      'A climber prepares to scale El Capitan with no ropes and no safety equipment.',
      'Elizabeth Chai Vasarhelyi and Jimmy Chin', 2018, 100, 7, '/images/covers/27.jpg', 5.00),
 (28, 'Searching for Sugar Man',
      'Two fans set out to discover what became of a forgotten American musician who had found fame elsewhere.',
      'Malik Bendjelloul', 2012, 86, 7, '/images/covers/28.jpg', 4.50);


-- ---------------------------------------------------------------------
-- Copies
--
-- Stock is deliberately uneven, and title 12 has exactly one copy so the
-- "last copy taken" path can be demonstrated without setup.
-- ---------------------------------------------------------------------
INSERT INTO copies (movie_id, barcode, status) VALUES
 (1,  'SRT-1-001',  'AVAILABLE'), (1,  'SRT-1-002',  'AVAILABLE'), (1,  'SRT-1-003',  'AVAILABLE'),
 (2,  'SRT-2-001',  'AVAILABLE'), (2,  'SRT-2-002',  'AVAILABLE'),
 (3,  'SRT-3-001',  'AVAILABLE'), (3,  'SRT-3-002',  'AVAILABLE'), (3,  'SRT-3-003',  'AVAILABLE'),
 (4,  'SRT-4-001',  'AVAILABLE'), (4,  'SRT-4-002',  'AVAILABLE'),
 (5,  'SRT-5-001',  'AVAILABLE'), (5,  'SRT-5-002',  'AVAILABLE'),
 (6,  'SRT-6-001',  'AVAILABLE'), (6,  'SRT-6-002',  'AVAILABLE'), (6,  'SRT-6-003',  'AVAILABLE'),
 (7,  'SRT-7-001',  'AVAILABLE'), (7,  'SRT-7-002',  'AVAILABLE'),
 (8,  'SRT-8-001',  'AVAILABLE'),
 (9,  'SRT-9-001',  'AVAILABLE'), (9,  'SRT-9-002',  'AVAILABLE'),
 (10, 'SRT-10-001', 'AVAILABLE'), (10, 'SRT-10-002', 'AVAILABLE'),
 (11, 'SRT-11-001', 'AVAILABLE'),
 (12, 'SRT-12-001', 'AVAILABLE'),
 (13, 'SRT-13-001', 'AVAILABLE'), (13, 'SRT-13-002', 'AVAILABLE'), (13, 'SRT-13-003', 'AVAILABLE'),
 (14, 'SRT-14-001', 'AVAILABLE'), (14, 'SRT-14-002', 'AVAILABLE'),
 (15, 'SRT-15-001', 'AVAILABLE'),
 (16, 'SRT-16-001', 'AVAILABLE'), (16, 'SRT-16-002', 'AVAILABLE'), (16, 'SRT-16-003', 'AVAILABLE'),
 (17, 'SRT-17-001', 'AVAILABLE'), (17, 'SRT-17-002', 'AVAILABLE'),
 (18, 'SRT-18-001', 'AVAILABLE'), (18, 'SRT-18-002', 'AVAILABLE'),
 (19, 'SRT-19-001', 'AVAILABLE'), (19, 'SRT-19-002', 'AVAILABLE'),
 (20, 'SRT-20-001', 'AVAILABLE'), (20, 'SRT-20-002', 'AVAILABLE'), (20, 'SRT-20-003', 'AVAILABLE'),
 (21, 'SRT-21-001', 'AVAILABLE'), (21, 'SRT-21-002', 'AVAILABLE'), (21, 'SRT-21-003', 'AVAILABLE'),
 (22, 'SRT-22-001', 'AVAILABLE'), (22, 'SRT-22-002', 'AVAILABLE'),
 (23, 'SRT-23-001', 'AVAILABLE'), (23, 'SRT-23-002', 'AVAILABLE'),
 (24, 'SRT-24-001', 'AVAILABLE'), (24, 'SRT-24-002', 'AVAILABLE'),
 (25, 'SRT-25-001', 'AVAILABLE'), (25, 'SRT-25-002', 'AVAILABLE'),
 (26, 'SRT-26-001', 'AVAILABLE'),
 (27, 'SRT-27-001', 'AVAILABLE'), (27, 'SRT-27-002', 'AVAILABLE'),
 (28, 'SRT-28-001', 'AVAILABLE');


-- ---------------------------------------------------------------------
-- Rental history
--
-- Three closed rentals to give the customers something to review, plus
-- one open and one deliberately overdue rental so the administrator's
-- returns screen and the late-fee calculation are visible immediately.
-- ---------------------------------------------------------------------

-- Closed: returned on time.
INSERT INTO rentals (copy_id, user_id, rented_at, due_date, returned_at, late_fee) VALUES
 ((SELECT id FROM copies WHERE barcode = 'SRT-1-001'), 2,
  DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 23 DAY),
  DATE_SUB(NOW(), INTERVAL 25 DAY), 0.00),
 ((SELECT id FROM copies WHERE barcode = 'SRT-6-001'), 2,
  DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 13 DAY),
  DATE_SUB(NOW(), INTERVAL 15 DAY), 0.00),
 ((SELECT id FROM copies WHERE barcode = 'SRT-9-001'), 3,
  DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(CURDATE(), INTERVAL 11 DAY),
  DATE_SUB(NOW(), INTERVAL 12 DAY), 0.00);

-- Open, still within its lending period.
INSERT INTO rentals (copy_id, user_id, rented_at, due_date) VALUES
 ((SELECT id FROM copies WHERE barcode = 'SRT-3-001'), 3,
  DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY));
UPDATE copies SET status = 'RENTED' WHERE barcode = 'SRT-3-001';

-- Open and four days overdue - accrues a late fee on the returns screen.
INSERT INTO rentals (copy_id, user_id, rented_at, due_date) VALUES
 ((SELECT id FROM copies WHERE barcode = 'SRT-13-001'), 2,
  DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY));
UPDATE copies SET status = 'RENTED' WHERE barcode = 'SRT-13-001';


-- ---------------------------------------------------------------------
-- Reviews - only for titles the reviewer actually rented, which is the
-- rule ReviewService enforces.
-- ---------------------------------------------------------------------
INSERT INTO reviews (movie_id, user_id, rating, comment) VALUES
 (1, 2, 5, 'A classic. It still holds up after all these years.'),
 (6, 2, 4, 'A moving film. A little long, but worth it.'),
 (9, 3, 5, 'Stunning animation, and it works just as well for adults.');
