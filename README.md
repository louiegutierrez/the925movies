# The925

**Team:** The925  
**Members:** Leonardo Gutierrez (`leonarlg`) and Martin Kimball (`mkimbal1`)  
**Demo:** [YouTube — Project 5](https://youtu.be/eAZoZS00x5k)

---

## Project Overview

Application is a full-stack movie browsing and purchasing application based on one of our class projects. The backend is a **Java Jakarta EE WAR** deployed on **Tomcat 10**, exposing a JSON REST API. The frontend is a **React + TypeScript SPA** (Vite + Tailwind CSS + shadcn/ui) that talks to the backend via `/api/*`.

---

## Local Setup (Start Here)

For a complete fresh-clone setup (database creation, seeding from `data/`, backend run options, frontend run, optional XML import, verification, troubleshooting), use:

- [`LOCAL_SETUP.md`](LOCAL_SETUP.md)

The guide uses:
- `src/main/sql/schema.sql` and `src/main/sql/stored-procedure.sql` (committed)
- `data/data.sql` and `data/stanford-movies/*` (not committed; see [`data/README.md`](data/README.md) for download instructions)

---

## 1 — Maven Build Profiles

| Profile | Source directory | Excluded webapp files | Use case |
|---------|-----------------|----------------------|----------|
| `default` | `src/main/java` | none | Full app (local dev, Docker) |
| `login` | `src/main/java/login` | All non-login pages | Login-only microservice |
| `movies` | `src/main/java/movies` | `login.html`, `login.js` | Movies microservice |

```bash
mvn clean package -Pdefault   # full app
mvn clean package -Plogin     # login service only
mvn clean package -Pmovies    # movies service only
```

---

## 2 — API Reference

### Login Service
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/login` | Authenticate user, sets JWT cookie |
| `GET` | `/api/login` | Redirects to login page |
| `GET` | `/api/recaptcha-site-key` | Returns reCAPTCHA site key for frontend |
| `GET` | `/api/logout` | Clears session and JWT cookie |

### Movies Service
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/genres` | List all genres |
| `GET` | `/api/search` | Search/filter movies (see params below) |
| `GET` | `/api/movie?id={id}` | Single movie detail |
| `GET` | `/api/star?id={id}` | Single star detail |
| `GET` | `/api/cart` | Get current cart |
| `POST` | `/api/cart` | Add/update cart item (`movieId`, `quantity`) |
| `POST` | `/api/payment` | Process checkout (`first_name`, `last_name`, `card_number`, `expiration_date`) |
| `GET` | `/api/payment` | Get order confirmation (clears cart) |
| `POST` | `/api/insert_star` | (Employee) Insert new star |
| `POST` | `/api/add_movie` | (Employee) Add movie via stored procedure |
| `GET` | `/api/get_metadata` | (Employee) DB schema metadata |

**`/api/search` query parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `title` | string | Partial title match |
| `year` | number | Exact year |
| `director` | string | Partial director match |
| `star` | string | Partial star name match |
| `genre` | string | Exact genre name |
| `letter` | string | First letter of title (`#` for non-alpha) |
| `page` | number | Page number (default: 1) |
| `size` | number | Results per page (default: 25) |
| `sort` | string | e.g. `rating_desc`, `title_asc`, `year_desc` |

---

## 3 — Frontend Architecture

The React frontend lives in `frontend/` and is completely independent of the Maven build.

```
frontend/
├── src/
│   ├── api/          # Typed fetch wrappers for all backend endpoints
│   ├── components/   # Navbar, MovieCard, Pagination, SortSelector, etc.
│   │   └── ui/       # shadcn/ui base components (Button, Input, Card, etc.)
│   ├── context/      # AuthContext (JWT cookie → role detection)
│   ├── hooks/        # useCart, useToast
│   └── pages/        # LoginPage, BrowsePage, MovieListPage, etc.
├── vite.config.ts    # Vite config — /api proxy to :8080
├── index.html
└── package.json
```

**Page → Route mapping:**

| Route | Page |
|-------|------|
| `/` | Login |
| `/browse` | Genre + A–Z browser |
| `/movies` | Movie list with search, sort, pagination |
| `/movie/:id` | Movie detail + cast |
| `/star/:id` | Star detail + filmography |
| `/cart` | Shopping cart |
| `/checkout` | Payment form |
| `/confirmation` | Order confirmation |
| `/dashboard` | Employee dashboard (role-gated) |

---

## 4 — XML Importer

Imports movie data from XML files into MySQL.

**Set up environment:**

```bash
export IMPORT_DB_URL='jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1'
export IMPORT_DB_USER='root'
export IMPORT_DB_PASSWORD='yourpassword'
```

**Run:**

```bash
cd xmlParser/xml-importer
mvn clean package
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target/xml-importer-1.0-SNAPSHOT.jar:target/lib/*" DOMDataImporter
```

If `IMPORT_DB_*` variables are unset, the importer falls back to `DB_MASTER_*` values.

---

## 5 — Kubernetes Deployment

Two manifests are provided. Each includes a `ConfigMap` for DB URLs and a `Secret` for credentials.

**Before applying**, replace all `replace-with-*` placeholders in the `Secret.stringData` section of the manifest.

```bash
# Single-node
kubectl apply -f the925.yaml

# Multi-node (master/slave DB, multiple replicas)
kubectl apply -f the925-multi.yaml
```

---

## 6 — Security Notes

- Do not commit `.env` or any file containing real secrets. `.gitignore` already excludes `.env` and `.env.*`.
- Rotate any secrets that were previously hardcoded in source history.
- Use Kubernetes Secrets or your CI/CD secret manager for production.
- The JWT cookie is `HttpOnly` — it cannot be read by JavaScript.

---

## 7 — Troubleshooting

| Symptom | Fix |
|---------|-----|
| `No plugin found for prefix 'tomcat7'` | Use Docker or local Tomcat 10 — see `LOCAL_SETUP.md` |
| `Missing required environment variable: JWT_SECRET_BASE64` | Set `JWT_SECRET_BASE64` and restart Tomcat/Docker |
| `Missing required environment variable: RECAPTCHA_SECRET_KEY` | Set `RECAPTCHA_SECRET_KEY` and restart |
| Login page shows "Error loading site key" | Set `RECAPTCHA_SITE_KEY`; confirm `GET /api/recaptcha-site-key` returns 200 |
| Datasource init failure on startup | Verify all `DB_*` variables are set and MySQL is reachable |
| Frontend shows blank page / API 404 | Ensure backend is running on `:8080` before starting `npm run dev` |
| Frontend shows 401 on every request | JWT cookie expired or missing — log in again |
| Importer connection failure | Set `IMPORT_DB_*` variables (or valid `DB_MASTER_*` fallbacks) |
