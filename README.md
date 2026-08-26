# Sartia — Video/DVD Rental System

A web system for managing a movie rental library, built in Java with **JSF**,
**JDBC** and **REST** on top of a **MySQL** database.

---

## Contents

- [Prerequisites           ](#prerequisites)
- [Installation and Running](#installation-and-running)
- [Demo Users              ](#demo-users)
- [System URLs             ](#system-urls)
- [Running the Tests       ](#running-the-tests)
  - [Integration-test database](#preparing-the-integration-test-database)
- [Configuration           ](#configuration)
- [Project Structure       ](#project-structure)
- [Troubleshooting         ](#troubleshooting)
- [Documentation           ](#documentation)

---

## Prerequisites

| Component | Version      | Check                                      |
|-----------|--------------|--------------------------------------------|
| JDK       | 17 or later  | `java -version`                            |
| Maven     | 3.8 or later | `mvn -version`                             |
| MySQL     | 8.0 or later | Must be running before starting the system |

The application server (Payara Micro) is downloaded automatically by Maven — no need to install it.

---

## Installation and Running

Commands are written for **PowerShell**. Where the Bash equivalent differs it is
given directly beneath, labelled *Bash*; where no alternative is shown, the
command is identical in both shells.

### Step 1 — Set up the database

Run both scripts in order: the first builds the schema, the second loads the
demo data.

PowerShell has no `<` input-redirection operator — it is reserved — so the
scripts are fed to `mysql` through `cmd`:

```powershell
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < db\schema.sql"
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < db\seed.sql"
```

If `mysql` is not on your PATH, add its directory for the current session
first:

```powershell
$env:Path += ";C:\Program Files\MySQL\MySQL Server 8.0\bin"
```

*Bash* — the redirection works directly:

```bash
mysql -u root -p --default-character-set=utf8mb4 < db/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < db/seed.sql
```

The scripts are re-runnable — `seed.sql` clears the tables before loading, so it can be used at any time to return the system to a clean demo state.

> **Important:** If the root password is not empty, update `db.password` in
> `src/main/resources/sartia.properties` before building — see
> [Configuration](#configuration).

### Step 2 — Build

```powershell
mvn clean package
```

This produces `target/sartia.war`. It also runs the unit tests, but not the
integration tests — those belong to `mvn verify` — so no database is needed to
build. To skip the tests entirely:

```powershell
mvn clean package -DskipTests
```

### Step 3 — Run

```powershell
mvn payara-micro:start
```

On the first run Maven downloads Payara Micro (about 100MB); subsequent runs are
immediate. The server is ready when this line appears in the console:

```
Payara Micro ... ready in ... (ms)
```

Then open in a browser:

```
http://localhost:8080/sartia/
```

To stop the server: `Ctrl+C`.

### Alternative — running without the Maven plugin

If the WAR is already built and you have a copy of `payara-micro.jar`:

```powershell
java -jar payara-micro.jar --deploy target/sartia.war --contextroot /sartia --port 8080
```

---

## Demo Users

| Username | Password   | Role                                                                  |
|----------|------------|-----------------------------------------------------------------------|
| `admin`  | `admin123` | Administrator — catalog, inventory and returns management             |
| `ahmad`  | `ahmad123` | Customer — has rental history, one open rental and one overdue rental |

The demo data includes 28 movies across 7 categories (four per category), 57
copies, both closed and open rentals, one deliberately overdue rental (to
demonstrate the late-fee calculation) and one movie with a single copy (to
demonstrate the "out of stock" state).

---

## System URLs

### User interface

| URL                           | Description               | Access        |
|-------------------------------|---------------------------|---------------|
| `/sartia/`                    | Movie catalog (home page) | Public        |
| `/sartia/movie.xhtml?id=1`    | Movie details             | Public        |
| `/sartia/login.xhtml`         | Login                     | Public        |
| `/sartia/register.xhtml`      | Registration              | Public        |
| `/sartia/my-rentals.xhtml`    | My rentals                | Customer      |
| `/sartia/admin/movies.xhtml`  | Catalog management        | Administrator |
| `/sartia/admin/returns.xhtml` | Open rentals              | Administrator |

### REST interface

| URL                               |       Description                                                    |
|-----------------------------------|----------------------------------------------------------------------|
| `/sartia/api/movies`              | Search the catalog (`q`, `categoryId`,  `available`, `page`, `size`) |
| `/sartia/api/movies/{id}`         | Movie details                                                        |
| `/sartia/api/movies/{id}/reviews` | Reviews for a movie                                                  |
| `/sartia/api/categories`          | Categories with movie counts                                         |

Examples:

```powershell
curl.exe "http://localhost:8080/sartia/api/categories"
curl.exe "http://localhost:8080/sartia/api/movies?available=true&size=5"
curl.exe "http://localhost:8080/sartia/api/movies/1"
```

Spell out `curl.exe`: in Windows PowerShell 5.1 a bare `curl` is an alias for
`Invoke-WebRequest`, which takes different arguments. To get parsed JSON back
rather than raw text, use `Invoke-RestMethod`:

```powershell
Invoke-RestMethod "http://localhost:8080/sartia/api/movies/1"
```

*Bash* — plain `curl`, otherwise identical:

```bash
curl "http://localhost:8080/sartia/api/movies/1"
```

Searching in Hebrew requires URL encoding. `%D7%9E%D7%98%D7%A8%D7%99%D7%A7%D7%A1`
is "מטריקס" (The Matrix):

```powershell
curl.exe "http://localhost:8080/sartia/api/movies?q=%D7%9E%D7%98%D7%A8%D7%99%D7%A7%D7%A1"
```

---

## Running the Tests

```powershell
# Unit tests only — 17 tests, no database needed
mvn test

# Everything — 33 tests (needs MySQL and the test schema, see below)
mvn verify
```

The suite is split across two Maven plugins: surefire runs the `*Test` classes
in the `test` phase, failsafe runs the `*IT` classes in the `verify` phase.

| Class                     |    Type     | Tests | Phase        |
|---------------------------|-------------|-------|--------------|
| `PasswordsTest`           |    Unit     |   6   | `mvn test`   |
| `RentalTest`              |    Unit     |   6   | `mvn test`   |
| `MovieSearchCriteriaTest` |    Unit     |   5   | `mvn test`   |
| `RentalLifecycleIT`       | Integration |   11  | `mvn verify` |
| `ConcurrentRentalIT`      | Integration |   5   | `mvn verify` |

`ConcurrentRentalIT` runs dozens of threads in parallel and verifies that no
double rental is created — this is the central correctness test of the system.

### Preparing the integration-test database

The integration tests insert real rows, so they run against a throwaway
`sartia_test` schema and never against the application's own `sartia` database
— a test run cannot leave fixture films in the demo catalogue. Create that
schema once:

```powershell
cmd /c "mysql -u root -p < scripts\setup-test-db.sql"
```

*Bash:*

```bash
mysql -u root -p < scripts/setup-test-db.sql
```

The connection details are the `test.db.url`, `test.db.user` and
`test.db.password` properties in `pom.xml`. Failsafe passes them to the forked
test JVM, where they override the `db.*` keys from `sartia.properties`. Either
edit them there or override on the command line:

```powershell
mvn verify "-Dtest.db.password=YOUR_PASSWORD"
```

To run one integration class, use `it.test` — plain `-Dtest` selects for
surefire, which no longer owns these classes:

```powershell
mvn verify -Dit.test=ConcurrentRentalIT
```

If an earlier run (before the schemas were separated) left fixture rows in the
real database, `scripts/cleanup-test-data.sql` removes them:

```powershell
cmd /c "mysql -u root -p < scripts\cleanup-test-data.sql"
```

### End-to-end test

Against an already running system:

```powershell
bash scripts/smoke-test.sh http://10.0.0.8:8080/sartia
```

The script itself is Bash, so on Windows it needs `bash` on the PATH — Git Bash
and WSL both provide it. The command is the same from either shell.

The script runs 22 checks: loading every page, the REST interface, access
control, login as customer and as administrator, and rejection of a wrong
password.

---

## Configuration

The file `src/main/resources/sartia.properties`:

```properties
db.url=jdbc:mysql://127.0.0.1:3306/sartia?useSSL=false&...
db.user=root
db.password=*****

db.pool.maxSize=10
db.pool.minIdle=2

rental.periodDays=7
rental.maxConcurrentPerUser=3
rental.lateFeePerDay=3.00
```

Every key can be overridden with a JVM runtime parameter, with no rebuild:

```powershell
java -Ddb.password=secret -Drental.periodDays=14 -jar payara-micro.jar ...
```

---

## Project Structure

```
project/
├── db/
│   ├── schema.sql                    Database schema (6 tables)
│   └── seed.sql                      Demo data (re-runnable: clears, then loads)
├── docs/
│   ├── סרטיה- מסמך תיאור פונקציונליות.pdf       Functional specification (Hebrew)
│   └── סרטיה-מסמך תכנון.pdf                     Design document (Hebrew)
├── scripts/
│   ├── setup-test-db.sql             Creates the sartia_test schema for the integration tests
│   ├── cleanup-test-data.sql         Removes fixture rows from a polluted database
│   └── smoke-test.sh                 End-to-end test against a running deployment
├── src/main/java/sartia/
│   ├── presentation/
│   │   ├── web/                      JSF backing beans (catalog, login, register, my-rentals, ...)
│   │   │   └── admin/                Admin beans (catalogue management, returns)
│   │   ├── rest/                     JAX-RS REST resources
│   │   │   └── dto/                  REST data-transfer objects
│   │   └── security/                 Authentication / authorization filter
│   ├── business/
│   │   ├── service/                  Business logic (catalog, rental, review, user)
│   │   ├── domain/                   Domain objects (Movie, Copy, Rental, User, ...)
│   │   └── exception/                Business exceptions
│   ├── persistence/
│   │   └── dao/                      Data access and transactions (JDBC)
│   └── common/
│       ├── config/                   App configuration and startup listener
│       └── util/                     Password hashing
├── src/main/resources/
│   ├── sartia.properties             Database and application settings
│   └── messages.properties           UI text
├── src/main/webapp/
│   ├── catalog.xhtml, movie.xhtml, login.xhtml, register.xhtml, my-rentals.xhtml, error.xhtml
│   │                                 Public and customer pages
│   ├── admin/                        Admin pages (movies, returns)
│   ├── resources/css/                Stylesheet
│   ├── images/covers/                Cover art
│   └── WEB-INF/
│       ├── templates/layout.xhtml    Shared page template and navigation
│       └── web.xml, beans.xml, faces-config.xml   Servlet, CDI and JSF configuration
├── src/test/java/sartia/             Unit tests and integration tests (*IT)
├── pom.xml                           Maven build (WAR, packaged with Payara Micro)
└── README.md
```

---

## Troubleshooting

### The server does not start — `Startup failed`

Usually MySQL is not running, or the schema was never created.

```powershell
mysql -u root -p -e "USE sartia; SHOW TABLES;"
```

Six tables should appear. If they do not, run `db/schema.sql`.

### `Access denied for user 'root'`

The root password does not match. Update `db.password` in `sartia.properties`,
or run with:

```powershell
mvn payara-micro:start -Ddb.password=YOUR_PASSWORD
```

Written without angle brackets on purpose: `<` is a reserved operator in
PowerShell, so typing `<password>` literally is a syntax error.

### Port 8080 is in use

```powershell
mvn payara-micro:start -Dpayara.microPort=8081
```

### Hebrew appears as question marks

The database must use `utf8mb4`. The schema sets this; if the database was
created manually:

```sql
ALTER DATABASE sartia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Integration tests fail

They need a running MySQL with the `sartia_test` schema installed — see
[Preparing the integration-test database](#preparing-the-integration-test-database).
`Unknown database 'sartia_test'` means that step was skipped;
`Access denied` means `test.db.password` in `pom.xml` does not match your root
password. To run only the unit tests, use `mvn test` instead of `mvn verify`.

### Resetting the demo data to a clean state

```powershell
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < db\seed.sql"
```

*Bash:*

```bash
mysql -u root -p --default-character-set=utf8mb4 < db/seed.sql
```

---

## Documentation

| Document                                   | Contents                                        |
|--------------------------------------------|------------------------------------------------ |
| docs/סרטיה- מסמך תיאור פונקציונליות.pdf  | The system from the user's point of view |
| docs/סרטיה-מסמך תכנון.pdf                 | System structure                         |

The most interesting entry point in the code is
`src/main/java/sartia/business/service/RentalService.java` — it holds the logic that prevents
double rentals, together with the reasoning behind each step.
