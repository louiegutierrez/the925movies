# Local Setup Guide

This guide walks through running the full project locally from a fresh clone, using the data already committed in `data/`.

## Prerequisites

- Java 11+
- Maven 3.8+
- Node.js 18+ and npm 9+
- MySQL 8.0+
- One backend runtime option:
  - Docker (recommended), or
  - Local Tomcat 10.1

## 1) Clone and enter the repo

```bash
git clone <your-repo-url>
cd cs122b-winter25-the-925
```

## 2) Configure environment variables

Copy and edit the env template:

```bash
cp .env.example .env
```

Update `.env` with real values. For local dev, set both master/slave DB vars to the same local MySQL instance:

```env
JWT_SECRET_BASE64=your-base64-secret
RECAPTCHA_SECRET_KEY=your-recaptcha-secret
RECAPTCHA_SITE_KEY=your-recaptcha-site-key

DB_MASTER_URL=jdbc:mysql://localhost:3306/moviedb?autoReconnect=true&allowPublicKeyRetrieval=true&useSSL=false&cachePrepStmts=true
DB_MASTER_USER=root
DB_MASTER_PASSWORD=yourpassword

DB_SLAVE_URL=jdbc:mysql://localhost:3306/moviedb?autoReconnect=true&allowPublicKeyRetrieval=true&useSSL=false&cachePrepStmts=true
DB_SLAVE_USER=root
DB_SLAVE_PASSWORD=yourpassword

IMPORT_DB_URL=jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1
IMPORT_DB_USER=root
IMPORT_DB_PASSWORD=yourpassword
```

## 3) Initialize and seed the database

The seed data files are not committed to git due to their size. Before proceeding,
make sure you have `data/data.sql` and `data/stanford-movies/` in place. See
[`data/README.md`](data/README.md) for download instructions.

From the repository root, run each command in order:

```bash
# Create the database and load the schema
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS moviedb;"
mysql -u root -p moviedb < src/main/sql/schema.sql

# Temporarily drop the price column so data.sql's 4-column movie inserts load cleanly
mysql -u root -p moviedb -e "ALTER TABLE movies DROP COLUMN price;"

# Load all seed data (~187K rows across movies, stars, genres, ratings, etc.)
mysql -u root -p moviedb < data/data.sql

# Re-add price column with randomized values (required by the running app)
mysql -u root -p moviedb -e "
  ALTER TABLE movies ADD COLUMN price DECIMAL(5,2) NOT NULL DEFAULT 0.00;
  UPDATE movies SET price = ROUND(RAND() * 195 + 5, 2);
"

# Load the stored procedure (references movies.price, so must come after the ALTER)
mysql -u root -p moviedb < src/main/sql/stored-procedure.sql
```

### 3a) Fix passwords and add an employee account

The seed data contains **plaintext** passwords (e.g. `keyboard`, `paper`), but the
app uses Jasypt's `StrongPasswordEncryptor` which expects hashed values. Login will
fail until you hash them.

First, build the project and generate a Jasypt hash for a known password:

```bash
mvn clean compile
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target/lib/*:target/classes" common.EncryptPassword password
```

This prints a hash string like `uA7M/...long-base64...`. Copy it and use it in the
two SQL commands below (replace `PASTE_HASH_HERE` with the actual output):

```bash
# Reset every customer password to "password"
mysql -u root -p moviedb -e "UPDATE customers SET password = 'PASTE_HASH_HERE';"

# Add a test employee account (password = "password")
mysql -u root -p moviedb -e "INSERT INTO employees (email, password) VALUES ('admin@fabflix.com', 'PASTE_HASH_HERE');"
```

After this step every customer account uses password **`password`** and the
employee dashboard is accessible with `admin@fabflix.com` / `password`.

## 4) Run the backend

Choose one option.

### Option A: Docker (recommended)

```bash
docker build -t fabflix-backend .
docker run --rm -p 8080:8080 --env-file .env fabflix-backend
```

Backend base URL: `http://localhost:8080`

> **Docker + local MySQL:** Inside the container, `localhost` means the container
> itself, not your host machine. If MySQL runs on your host, change the
> `DB_*_URL` values in `.env` to use `host.docker.internal` instead of
> `localhost` (macOS/Windows) or `172.17.0.1` (Linux).

### Option B: Local Tomcat 10.1

Build WAR:

```bash
mvn clean package -Pdefault
```

Copy WAR to Tomcat:

```bash
cp target/cs122b-project5-the925.war ~/tomcat10/webapps/ROOT.war
```

Create `~/tomcat10/bin/setenv.sh` with the same env vars from `.env` (exported), then:

```bash
chmod +x ~/tomcat10/bin/setenv.sh
~/tomcat10/bin/catalina.sh run
```

Backend base URL: `http://localhost:8080`

## 5) Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

The Vite dev server proxies `/api/*` to `http://localhost:8080`.

## 6) Optional: run XML importer with local `data/stanford-movies`

The importer expects a local `stanford-movies/` folder in its current working directory. Since your XML lives in `data/stanford-movies`, copy it once:

```bash
cd xmlParser/xml-importer
cp -R ../../data/stanford-movies .
```

Build and run importer:

```bash
mvn clean package
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
export IMPORT_DB_URL="jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1"
export IMPORT_DB_USER="root"
export IMPORT_DB_PASSWORD="yourpassword"
java -cp "target/xml-importer-1.0-SNAPSHOT.jar:target/lib/*" DOMDataImporter
```

Output logs are written in `xmlParser/xml-importer/`:
- `dom_import_summary.txt`
- `inconsistent_mains243.txt`
- `inconsistent_actors63.txt`
- `inconsistent_casts124.txt`
- `duplicate_movies.txt`
- `duplicate_stars.txt`

## 7) Quick verification checklist

- Backend health check:
  - Open `http://localhost:8080/api/genres`
  - Expect a JSON list response
- Frontend check:
  - Open `http://localhost:5173`
  - Login page renders
- Search check:
  - Browse/search returns movie results
- Checkout check:
  - Add item to cart, proceed to checkout

## 8) Troubleshooting

- `Datasource init failure`:
  - Verify `DB_MASTER_*` and `DB_SLAVE_*` values and MySQL connectivity.
- Backend starts but login fails:
  - Ensure `JWT_SECRET_BASE64` is non-empty and restart backend.
  - Make sure you ran step 3a to hash the plaintext passwords in the seed data.
- `Incorrect email or password` for every customer:
  - The seed data has plaintext passwords. Run the UPDATE in step 3a, then
    log in with any customer email and password `password`.
- Cannot log in to the employee dashboard:
  - The seed data has no employee rows. Run the INSERT in step 3a.
- `Column count doesn't match value count at row ...` while importing `data.sql`:
  - You likely loaded `data.sql` before dropping the `price` column. Drop and
    recreate the database, then follow step 3 exactly in order.
- Frontend API 404/500:
  - Confirm backend is running on `:8080` and frontend on `:5173`.
- Importer cannot find XML files:
  - Confirm `xmlParser/xml-importer/stanford-movies/{mains243.xml,actors63.xml,casts124.xml}` exists.
- Importer DB auth errors:
  - Verify `IMPORT_DB_*` (or fallback `DB_MASTER_*`) env vars.
