# Sartia — Video/DVD Rental System

Final project · The Open University of Israel

A web system for managing a movie rental library, built in Java with **JSF**,
**JDBC** and **REST** on top of a **MySQL** database.

---

## Contents

- [Prerequisites](#prerequisites)
- [Installation and Running](#installation-and-running)
- [Demo Users](#demo-users)
- [System URLs](#system-urls)
- [Running the Tests](#running-the-tests)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)

---

## Prerequisites

| Component | Version | Check |
|-----------|---------|-------|
| JDK | 17 or later | `java -version` |
| Maven | 3.8 or later | `mvn -version` |
| MySQL | 8.0 or later | Must be running before starting the system |

The application server (Payara Micro) is downloaded automatically by Maven — no
need to install it.

---

## Installation and Running

### Step 1 — Set up the database

Run both scripts in order: the first builds the schema, the second loads the
demo data.

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/seed.sql
```

On Windows, if `mysql` is not on your PATH, use the full path:

```
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < db\schema.sql
```

In PowerShell the `<` operator is not supported. Run the command through `cmd`
instead:

```powershell
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < db\schema.sql"
```

The scripts are re-runnable — `seed.sql` clears the tables before loading, so it
can be used at any time to return the system to a clean demo state.

> **Important:** If the root password is not empty, update `db.password` in
> `src/main/resources/sartia.properties` before building — see
> [Configuration](#configuration).

### Step 2 — Build

```bash
mvn clean package
```

This produces `target/sartia.war`. It also runs the full test suite, including
integration tests that require a running MySQL. To skip the tests:

```bash
mvn clean package -DskipTests
```

### Step 3 — Run

```bash
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

```bash
java -jar payara-micro.jar --deploy target/sartia.war --contextroot /sartia --port 8080
```

---

## Demo Users

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | Administrator — catalog, inventory and returns management |
| `david` | `david123` | Customer — has rental history and one overdue rental |
| `noa` | `noa123` | Customer — has one open rental in good standing |

The demo data includes 28 movies across 7 categories (four per category), 57
copies, both closed and open rentals, one deliberately overdue rental (to
demonstrate the late-fee calculation) and one movie with a single copy (to
demonstrate the "out of stock" state).

---

## System URLs

### User interface

| URL | Description | Access |
|-----|-------------|--------|
| `/sartia/` | Movie catalog (home page) | Public |
| `/sartia/movie.xhtml?id=1` | Movie details | Public |
| `/sartia/login.xhtml` | Login | Public |
| `/sartia/register.xhtml` | Registration | Public |
| `/sartia/my-rentals.xhtml` | My rentals | Customer |
| `/sartia/admin/movies.xhtml` | Catalog management | Administrator |
| `/sartia/admin/returns.xhtml` | Open rentals | Administrator |

### REST interface

| URL | Description |
|-----|-------------|
| `/sartia/api/movies` | Search the catalog (`q`, `categoryId`, `available`, `page`, `size`) |
| `/sartia/api/movies/{id}` | Movie details |
| `/sartia/api/movies/{id}/reviews` | Reviews for a movie |
| `/sartia/api/categories` | Categories with movie counts |

Examples:

```bash
curl "http://localhost:8080/sartia/api/categories"
curl "http://localhost:8080/sartia/api/movies?available=true&size=5"
curl "http://localhost:8080/sartia/api/movies/1"
```

Searching in Hebrew requires URL encoding. `%D7%9E%D7%98%D7%A8%D7%99%D7%A7%D7%A1`
is "מטריקס" (The Matrix):

```bash
curl "http://localhost:8080/sartia/api/movies?q=%D7%9E%D7%98%D7%A8%D7%99%D7%A7%D7%A1"
```

---

## Running the Tests

```bash
# All tests — 33 tests (requires a running MySQL)
mvn test

# Unit tests only — no database needed
mvn test -Dgroups='!integration'

# Concurrency tests only
mvn test -Dtest=ConcurrentRentalIT
```

| Class | Type | Tests |
|-------|------|-------|
| `PasswordsTest` | Unit | 6 |
| `RentalTest` | Unit | 6 |
| `MovieSearchCriteriaTest` | Unit | 5 |
| `RentalLifecycleIT` | Integration | 11 |
| `ConcurrentRentalIT` | Integration | 5 |

`ConcurrentRentalIT` runs dozens of threads in parallel and verifies that no
double rental is created — this is the central correctness test of the system.

### End-to-end test

Against an already running system:

```bash
bash scripts/smoke-test.sh
```

The script runs 22 checks: loading every page, the REST interface, access
control, login as customer and as administrator, and rejection of a wrong
password.

---

## Configuration

The file `src/main/resources/sartia.properties`:

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

Every key can be overridden with a JVM runtime parameter, with no rebuild:

```bash
java -Ddb.password=secret -Drental.periodDays=14 -jar payara-micro.jar ...
```

---

## Project Structure

```
project/
├── db/
│   ├── schema.sql              Database schema
│   └── seed.sql                Demo data
├── docs/
│   ├── 01-מסמך-פונקציונליות.md  Functional specification
│   └── 02-מסמך-תכנון.md         Design document
├── scripts/
│   └── smoke-test.sh           End-to-end test
├── src/main/java/
│   ├── model/                  Domain objects
│   ├── dao/                    Data access and transactions
│   ├── service/                Business logic
│   ├── web/                    JSF backing beans
│   ├── rest/                   REST interface
│   └── util/                   Passwords and configuration
├── src/main/webapp/            XHTML pages, CSS and configuration
├── src/test/java/              Tests
├── pom.xml
└── README.md
```

---

## Troubleshooting

### The server does not start — `Startup failed`

Usually MySQL is not running, or the schema was never created.

```bash
mysql -u root -p -e "USE sartia; SHOW TABLES;"
```

Six tables should appear. If they do not, run `db/schema.sql`.

### `Access denied for user 'root'`

The root password does not match. Update `db.password` in `sartia.properties`,
or run with:

```bash
mvn payara-micro:start -Ddb.password=<password>
```

### Port 8080 is in use

```bash
mvn payara-micro:start -Dpayara.microPort=8081
```

### Hebrew appears as question marks

The database must use `utf8mb4`. The schema sets this; if the database was
created manually:

```sql
ALTER DATABASE sartia CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Integration tests fail

They require a running MySQL with the schema installed. To run only the unit
tests:

```bash
mvn test -Dgroups='!integration'
```

### Resetting the demo data to a clean state

```bash
mysql -u root -p < db/seed.sql
```

---

## Documentation

| Document | Contents |
|----------|----------|
| [`docs/01-מסמך-פונקציונליות.md`](docs/01-מסמך-פונקציונליות.md) | The system from the user's point of view: every screen, every action, the business rules and the API |
| [`docs/02-מסמך-תכנון.md`](docs/02-מסמך-תכנון.md) | System structure: architecture, ERD, description of every class, concurrency handling and security |

The most interesting entry point in the code is
`src/main/java/service/RentalService.java` — it holds the logic that prevents
double rentals, together with the reasoning behind each step.
