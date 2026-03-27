# The925

## Team
- Team: The925
- Names: Leonardo Gutierrez (`leonarlg`) and Martin Kimball (`mkimbal1`)
- Project 5 demo: [YouTube link](https://youtu.be/eAZoZS00x5k)

## Service Distribution
### Login Service
- `POST /api/login`
- `GET /api/login`
- `GET /api/recaptcha-site-key`

### Movies Service
- `POST /api/add_movie`
- `GET /api/cart`
- `POST /api/cart`
- `GET /api/genres`
- `GET /api/get_metadata`
- `POST /api/insert_star`
- `GET /api/logout`
- `POST /api/payment`
- `GET /api/payment`
- `GET /api/search`
- `GET /api/movie?id={movieId}`
- `GET /api/star?id={starId}`

## Environment Variables
Copy `.env.example` to `.env` and replace placeholder values before running.

Required variables:
- `JWT_SECRET_BASE64`: Base64-encoded JWT signing key (HS256, sufficiently long).
- `RECAPTCHA_SECRET_KEY`: Server-side Google reCAPTCHA secret.
- `RECAPTCHA_SITE_KEY`: Client-side Google reCAPTCHA site key.
- `DB_MASTER_URL`: JDBC URL for master DB datasource.
- `DB_MASTER_USER`: Username for master DB datasource.
- `DB_MASTER_PASSWORD`: Password for master DB datasource.
- `DB_SLAVE_URL`: JDBC URL for slave DB datasource.
- `DB_SLAVE_USER`: Username for slave DB datasource.
- `DB_SLAVE_PASSWORD`: Password for slave DB datasource.

Importer variables:
- `IMPORT_DB_URL`: JDBC URL for importer database connection.
- `IMPORT_DB_USER`: Importer DB username.
- `IMPORT_DB_PASSWORD`: Importer DB password.

Importer fallback behavior:
- If `IMPORT_DB_*` variables are unset, importer falls back to `DB_MASTER_URL`, `DB_MASTER_USER`, and `DB_MASTER_PASSWORD`.

## Local Build
Build with Maven profile:

```bash
mvn clean package -Pdefault
```

Alternative profiles:
- `-Plogin`
- `-Pmovies`

## Docker Build
The Dockerfile uses `MVN_PROFILE` build arg:

```bash
docker build --build-arg MVN_PROFILE=default -t the925:local .
```

## Runtime Configuration
### Tomcat JNDI datasource config
`src/main/webapp/META-INF/context.xml` is now env-backed:
- `${env.DB_MASTER_URL}`
- `${env.DB_MASTER_USER}`
- `${env.DB_MASTER_PASSWORD}`
- `${env.DB_SLAVE_URL}`
- `${env.DB_SLAVE_USER}`
- `${env.DB_SLAVE_PASSWORD}`

Tomcat resolves these from container environment variables at startup.

### Java secret loading
- `JwtUtil` reads `JWT_SECRET_BASE64`.
- `RecaptchaVerifyUtils` reads `RECAPTCHA_SECRET_KEY`.
- `RecaptchaConfigServlet` exposes `RECAPTCHA_SITE_KEY` to the login page via `GET /api/recaptcha-site-key`.

If required values are missing, startup/request handling fails with clear error messages.

## Kubernetes
Manifests:
- `the925.yaml`
- `the925-multi.yaml`

Both manifests now include:
- A `ConfigMap` (`the925-config`) for DB URLs.
- A `Secret` (`the925-secrets`) for DB credentials and app secrets.
- `envFrom` wiring in Deployments.

Before applying to production, replace all `replace-with-*` placeholders in manifest `Secret.stringData`.

Example:

```bash
kubectl apply -f the925.yaml
```

or

```bash
kubectl apply -f the925-multi.yaml
```

## XML Importer
Importer entrypoint:
- `xmlParser/xml-importer/src/main/java/com/fabflix/importer/DOMDataImporter.java`

Run with env vars:

```bash
export IMPORT_DB_URL='jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1'
export IMPORT_DB_USER='your-user'
export IMPORT_DB_PASSWORD='your-password'
```

## Security Notes
- Do not commit `.env` files. `.gitignore` excludes `.env` and `.env.*` (except `.env.example`).
- Previously hardcoded secrets should be rotated because they were exposed in source history.
- Prefer Kubernetes Secrets or your CI/CD secret manager for production values.

## Troubleshooting
- **`Missing required environment variable: JWT_SECRET_BASE64`**
  - Set `JWT_SECRET_BASE64` in runtime environment and restart.
- **`Missing required environment variable: RECAPTCHA_SECRET_KEY`**
  - Set `RECAPTCHA_SECRET_KEY` in runtime environment and restart.
- **Login page error loading site key**
  - Ensure `RECAPTCHA_SITE_KEY` is set and `GET /api/recaptcha-site-key` returns success.
- **Datasource init failure**
  - Verify all `DB_*` variables are set and JDBC URLs are reachable from the container.
- **Importer connection failure**
  - Set `IMPORT_DB_*` (or valid `DB_MASTER_*` fallback values) before running importer.
