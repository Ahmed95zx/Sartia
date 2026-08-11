-- =====================================================================
--  Sartia - demonstration data
--
--  Run after schema.sql. Safe to re-run: it clears the tables it fills.
--
--  The password_hash values below are genuine PBKDF2-HMAC-SHA256 hashes
--  produced by il.ac.openu.sartia.util.Passwords, not placeholders. To
--  mint new ones:
--      java -cp target/classes il.ac.openu.sartia.util.Passwords <password>
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
     'admin@sartia.co.il',  'מנהל המערכת', '03-5555555', 'ADMIN'),
 (2, 'david', '210000:IImw9y8E76IwAiEGvpIxMQ==:+4OLVh5dvDOSpIVl5EXUN1uDh4M1NSQZ3w9cZUhq9Sc=',
     'david@example.com',   'דוד כהן',     '052-1234567', 'CUSTOMER'),
 (3, 'noa',   '210000:h1HLfjSWTjX0wILDaI1Vmg==:65YQidWEz2T1ay9Hqym2PUc6oB5Oa9tBf2zg9U1cw6A=',
     'noa@example.com',     'נועה לוי',    '054-7654321', 'CUSTOMER');


-- ---------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------
INSERT INTO categories (id, name, description) VALUES
 (1, 'אקשן',      'סרטי פעולה, מתח ומרדפים'),
 (2, 'קומדיה',    'סרטים קלילים ומצחיקים'),
 (3, 'דרמה',      'סרטי עלילה ורגש'),
 (4, 'מדע בדיוני','עתידנות, חלל וטכנולוגיה'),
 (5, 'אנימציה',   'סרטי הנפשה למשפחה'),
 (6, 'אימה',      'סרטי מתח ואימה'),
 (7, 'תעודי',     'סרטים דוקומנטריים');


-- ---------------------------------------------------------------------
-- Movies
-- ---------------------------------------------------------------------
INSERT INTO movies (id, title, description, director, release_year, duration_min, category_id, daily_price) VALUES
 (1,  'המטריקס',
      'האקר מגלה שהמציאות שהוא מכיר היא סימולציה, ומצטרף למרד נגד המכונות ששולטות באנושות.',
      'האחיות ואצ''ובסקי', 1999, 136, 4, 6.00),
 (2,  'בלייד ראנר 2049',
      'שוטר צעיר חושף סוד קבור שעלול לשנות את מה שנותר מהחברה האנושית.',
      'דני וילנב', 2017, 164, 4, 7.00),
 (3,  'האביר האפל',
      'באטמן מתמודד עם הג''וקר, פושע כאוטי שמאיים לשתק את גות''אם סיטי.',
      'כריסטופר נולאן', 2008, 152, 1, 6.50),
 (4,  'מקס הזועם: כביש הזעם',
      'מרדף בלתי פוסק במדבר פוסט-אפוקליפטי, בניסיון להימלט ממצביא אכזר.',
      'ג''ורג'' מילר', 2015, 120, 1, 6.00),
 (5,  'משחק ההעתקה',
      'אלן טיורינג מוביל את הצוות שפיצח את צופן האניגמה במלחמת העולם השנייה.',
      'מורטן טילדום', 2014, 114, 3, 5.50),
 (6,  'פורסט גאמפ',
      'אדם תמים חוצה עשורים של היסטוריה אמריקאית מבלי להתכוון לכך.',
      'רוברט זמקיס', 1994, 142, 3, 5.00),
 (7,  'הגראנד בודפשט הוטל',
      'קונסיירז'' אגדי ושוליה נקלעים לפרשת רצח וירושה באירופה שבין המלחמות.',
      'וס אנדרסון', 2014, 99, 2, 5.50),
 (8,  'סופרבד',
      'שני חברים מנסים לשרוד את הלילה האחרון של התיכון.',
      'גרג מוטולה', 2007, 113, 2, 4.50),
 (9,  'מעלה הרוח',
      'ילדה נלכדת בעולם רוחות ונאבקת להציל את הוריה ולחזור הביתה.',
      'האיאו מיאזאקי', 2001, 125, 5, 5.50),
 (10, 'מפלצות בע"מ',
      'שתי מפלצות מגלות ילדה אנושית ומגלים שכל מה שלימדו אותם היה שגוי.',
      'פיט דוקטר', 2001, 92, 5, 5.00),
 (11, 'תפוס אותי אם תוכל',
      'נוכל צעיר מתחזה לטייס, לרופא ולעורך דין בעוד סוכן FBI במרדף אחריו.',
      'סטיבן ספילברג', 2002, 141, 3, 5.00),
 (12, 'קלטת',
      'עיתונאית חוקרת קלטת וידאו מקוללת שהצופים בה מתים כעבור שבוע.',
      'גור ורבינסקי', 2002, 115, 6, 5.50),
 (13, 'הבריחה משוושנק',
      'בנקאי שהורשע ברצח שלא ביצע בונה במשך שנים את דרכו לחופש.',
      'פרנק דארבונט', 1994, 142, 3, 6.00),
 (14, 'כוכב הקופים: המרד',
      'קופים בעלי תבונה מתעמתים עם שרידי האנושות על השליטה בכדור הארץ.',
      'מאט ריבס', 2014, 130, 4, 5.50),
 (15, 'הליכה על חבל',
      'תיעוד המסע של אמן חבל מתוח בדרך למתיחת חבל בין מגדלי התאומים.',
      'ג''יימס מארש', 2008, 94, 7, 4.50);


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
 (15, 'SRT-15-001', 'AVAILABLE');


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
 (1, 2, 5, 'קלאסיקה. עדיין מחזיק מעמד אחרי כל השנים.'),
 (6, 2, 4, 'סרט מרגש, קצת ארוך אבל שווה.'),
 (9, 3, 5, 'אנימציה מדהימה, מתאים גם למבוגרים.');
