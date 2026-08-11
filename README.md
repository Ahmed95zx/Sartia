# סרטייה — מערכת השאלת סרטי וידאו/DVD

פרויקט גמר · האוניברסיטה הפתוחה

מערכת Web לניהול ספריית השאלות של סרטים, הבנויה ב-Java עם **JSF**, **JDBC**
ו-**REST** מעל בסיס נתונים **MySQL**.

---

## תוכן

- [דרישות מוקדמות](#דרישות-מוקדמות)
- [התקנה והרצה](#התקנה-והרצה)
- [משתמשי הדגמה](#משתמשי-הדגמה)
- [כתובות המערכת](#כתובות-המערכת)
- [הרצת הבדיקות](#הרצת-הבדיקות)
- [הגדרות](#הגדרות)
- [מבנה הפרויקט](#מבנה-הפרויקט)
- [פתרון תקלות](#פתרון-תקלות)
- [תיעוד](#תיעוד)

---

## דרישות מוקדמות

| רכיב | גרסה | הערה |
|-------|-------|------|
| JDK | 17 ומעלה | `java -version` |
| Maven | 3.8 ומעלה | `mvn -version` |
| MySQL | 8.0 ומעלה | חייב לרוץ לפני הפעלת המערכת |

שרת היישומים (Payara Micro) יורד אוטומטית על ידי Maven — אין צורך להתקינו.

---

## התקנה והרצה

### שלב 1 — הקמת בסיס הנתונים

יש להריץ את שני הסקריפטים לפי הסדר: הראשון בונה את הסכמה, השני טוען נתוני
הדגמה.

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

בסביבת Windows, אם `mysql` אינו ב-PATH, יש להשתמש בנתיב המלא:

```
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < db\schema.sql
```

הסקריפטים ניתנים להרצה חוזרת — `seed.sql` מנקה את הטבלאות לפני הטעינה, ולכן
ניתן להשתמש בו כדי להחזיר את המערכת למצב הדגמה נקי בכל עת.

> **חשוב:** אם סיסמת ה-root אינה ריקה, יש לעדכן את `db.password` בקובץ
> `src/main/resources/sartia.properties` לפני הבנייה — ראו [הגדרות](#הגדרות).

### שלב 2 — בנייה

```bash
mvn clean package
```

הפקודה מייצרת את `target/sartia.war`. היא גם מריצה את כל הבדיקות, ובכללן
בדיקות אינטגרציה הזקוקות ל-MySQL פעיל. לדילוג על הבדיקות:

```bash
mvn clean package -DskipTests
```

### שלב 3 — הרצה

```bash
mvn payara-micro:start
```

בהפעלה הראשונה Maven מוריד את שרת Payara Micro (כ-100MB); ההפעלות הבאות
מיידיות. השרת מוכן כאשר מופיעה בקונסולה השורה:

```
Payara Micro ... ready in ... (ms)
```

יש לפתוח בדפדפן:

```
http://localhost:8080/sartia/
```

לעצירת השרת: `Ctrl+C`.

### הרצה חלופית — ללא תוסף Maven

אם קובץ ה-WAR כבר נבנה וקיים עותק של `payara-micro.jar`:

```bash
java -jar payara-micro.jar --deploy target/sartia.war --contextroot /sartia --port 8080
```

---

## משתמשי הדגמה

| שם משתמש | סיסמה | תפקיד |
|-----------|--------|--------|
| `admin` | `admin123` | מנהלן — ניהול קטלוג, מלאי והחזרות |
| `david` | `david123` | לקוח — עם היסטוריית השאלות והשאלה באיחור |
| `noa` | `noa123` | לקוח — עם השאלה פתוחה בתוקף |

נתוני ההדגמה כוללים 15 סרטים ב-7 קטגוריות, 30 עותקים, השאלות סגורות
ופתוחות, השאלה אחת באיחור מכוון (להצגת חישוב הקנס) וסרט אחד בעל עותק יחיד
(להדגמת מצב "אזל המלאי").

---

## כתובות המערכת

### ממשק המשתמש

| כתובת | תיאור | הרשאה |
|--------|--------|--------|
| `/sartia/` | קטלוג הסרטים (דף הבית) | פתוח |
| `/sartia/movie.xhtml?id=1` | פרטי סרט | פתוח |
| `/sartia/login.xhtml` | כניסה | פתוח |
| `/sartia/register.xhtml` | הרשמה | פתוח |
| `/sartia/my-rentals.xhtml` | ההשאלות שלי | לקוח |
| `/sartia/admin/movies.xhtml` | ניהול קטלוג | מנהלן |
| `/sartia/admin/returns.xhtml` | השאלות פתוחות | מנהלן |

### ממשק ה-REST

| כתובת | תיאור |
|--------|--------|
| `/sartia/api/movies` | חיפוש בקטלוג (`q`, `categoryId`, `available`, `page`, `size`) |
| `/sartia/api/movies/{id}` | פרטי סרט |
| `/sartia/api/movies/{id}/reviews` | ביקורות על סרט |
| `/sartia/api/categories` | קטגוריות עם מונה סרטים |

דוגמאות:

```bash
curl "http://localhost:8080/sartia/api/categories"
curl "http://localhost:8080/sartia/api/movies?available=true&size=5"
curl "http://localhost:8080/sartia/api/movies/1"
```

חיפוש בעברית מחייב קידוד URL. `%D7%9E%D7%98%D7%A8%D7%99%D7%A7%D7%A1` הוא
"מטריקס":

```bash
curl "http://localhost:8080/sartia/api/movies?q=%D7%9E%D7%98%D7%A8%D7%99%D7%A7%D7%A1"
```

---

## הרצת הבדיקות

```bash
# כל הבדיקות — 33 בדיקות (מחייב MySQL פעיל)
mvn test

# בדיקות יחידה בלבד — ללא צורך בבסיס נתונים
mvn test -Dgroups='!integration'

# בדיקות המקביליות בלבד
mvn test -Dtest=ConcurrentRentalIT
```

| מחלקה | סוג | בדיקות |
|--------|------|--------|
| `PasswordsTest` | יחידה | 6 |
| `RentalTest` | יחידה | 6 |
| `MovieSearchCriteriaTest` | יחידה | 5 |
| `RentalLifecycleIT` | אינטגרציה | 11 |
| `ConcurrentRentalIT` | אינטגרציה | 5 |

`ConcurrentRentalIT` מפעילה עשרות חוטים במקביל ומוודאת שלא נוצרת השאלה
כפולה — זו הבדיקה המרכזית של נכונות המערכת.

### בדיקת קצה-לקצה

מול מערכת שכבר רצה:

```bash
bash scripts/smoke-test.sh
```

הסקריפט מריץ 22 בדיקות: טעינת כל הדפים, ממשק ה-REST, בקרת הגישה, כניסה
כלקוח וכמנהלן ודחיית סיסמה שגויה.

---

## הגדרות

הקובץ `src/main/resources/sartia.properties`:

```properties
db.url=jdbc:mysql://127.0.0.1:3306/sartia?useSSL=false&...
db.user=root
db.password=

db.pool.maxSize=10
db.pool.minIdle=2

rental.periodDays=7
rental.maxConcurrentPerUser=3
rental.lateFeePerDay=3.00
```

כל מפתח ניתן לדריסה בפרמטר הרצה של ה-JVM, ללא בנייה מחדש:

```bash
java -Ddb.password=secret -Drental.periodDays=14 -jar payara-micro.jar ...
```

---

## מבנה הפרויקט

```
project/
├── db/
│   ├── schema.sql              סכמת בסיס הנתונים
│   └── seed.sql                נתוני הדגמה
├── docs/
│   ├── 01-מסמך-פונקציונליות.md  תיאור המערכת למשתמש
│   └── 02-מסמך-תכנון.md         מבנה המערכת והמחלקות
├── scripts/
│   └── smoke-test.sh           בדיקת קצה-לקצה
├── src/main/java/il/ac/openu/sartia/
│   ├── model/                  אובייקטי התחום
│   ├── dao/                    גישה לנתונים וטרנזקציות
│   ├── service/                לוגיקה עסקית
│   ├── web/                    JSF Backing Beans
│   ├── rest/                   ממשק REST
│   └── util/                   סיסמאות והגדרות
├── src/main/webapp/            דפי XHTML, CSS והגדרות
├── src/test/java/              בדיקות
├── pom.xml
└── README.md
```

---

## פתרון תקלות

### השרת אינו עולה — `Startup failed`

בדרך כלל MySQL אינו פעיל או שהסכמה לא הוקמה.

```bash
mysql -u root -p -e "USE sartia; SHOW TABLES;"
```

אמורות להופיע שש טבלאות. אם לא — יש להריץ את `db/schema.sql`.

### `Access denied for user 'root'`

סיסמת ה-root אינה תואמת. יש לעדכן את `db.password` ב-`sartia.properties`,
או להריץ עם:

```bash
mvn payara-micro:start -Ddb.password=<הסיסמה>
```

### פורט 8080 תפוס

```bash
mvn payara-micro:start -Dpayara.microPort=8081
```

### עברית מוצגת כסימני שאלה

בסיס הנתונים חייב להיות ב-`utf8mb4`. הסכמה מגדירה זאת; אם הבסיס נוצר ידנית:

```sql
ALTER DATABASE sartia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### בדיקות האינטגרציה נכשלות

הן מחייבות MySQL פעיל עם הסכמה מותקנת. להרצת בדיקות היחידה בלבד:

```bash
mvn test -Dgroups='!integration'
```

### החזרת נתוני ההדגמה למצב נקי

```bash
mysql -u root -p < db/seed.sql
```

---

## תיעוד

| מסמך | תוכן |
|-------|-------|
| [`docs/01-מסמך-פונקציונליות.md`](docs/01-מסמך-פונקציונליות.md) | תיאור המערכת מנקודת מבט המשתמש: כל מסך, כל פעולה, החוקים העסקיים וממשק ה-API |
| [`docs/02-מסמך-תכנון.md`](docs/02-מסמך-תכנון.md) | מבנה המערכת: ארכיטקטורה, ERD, תיאור כל המחלקות, טיפול במקביליות ואבטחה |

נקודת הכניסה המעניינת בקוד היא
`src/main/java/il/ac/openu/sartia/service/RentalService.java` — שם מרוכזת
הלוגיקה המונעת השאלה כפולה, יחד עם הנימוק לכל שלב.
