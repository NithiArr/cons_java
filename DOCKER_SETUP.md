# Docker Setup – Step by Step

Your app runs in **3 separate containers**:

| Container | Role | Image / Build |
|-----------|------|---------------|
| **db** | PostgreSQL database | `postgres:15` (official) |
| **backend** | Django app (API, logic, templates) | Built from `Dockerfile.backend` |
| **frontend** | Nginx (serves static files, proxies to backend) | `nginx:alpine` (official) |

Traffic flow: **Browser → frontend (port 80) → backend or static files**.

---

## Step 1: Start the database container (no build needed)

The database uses the official PostgreSQL image. There is no custom Dockerfile for it.

```bash
docker compose up -d db
```

What happens: PostgreSQL 15 starts in a container named `construction-db`, creates the empty database `construction_db`, and stores data in the `postgres_data` volume.

You can check with:

```bash
docker compose ps
docker compose logs db
```

---

## Step 2: Build and start all containers

```bash
docker compose up -d --build
```

This will:

1. **db**: Start PostgreSQL (from `postgres:15`).
2. **backend**: Build from `Dockerfile.backend` (install deps, run collectstatic), then start Django with Gunicorn.
3. **frontend**: Start Nginx using `nginx/nginx.conf` (serves static, proxies to backend).

---

## Step 3: Create tables like your local database (migrations)

Tables are created by Django migrations, not by a custom database image.

```bash
docker compose exec backend python manage.py migrate
```

This creates all tables in `construction_db`, matching your Django models (users, company, project, expense, etc.).

---

## Step 4: Migrate your local data into the database container

Copy data from your **local PostgreSQL** into the container.

### 4a. Dump your local database

If you have local PostgreSQL (or Docker with the database):

```bash
# Local PostgreSQL (on host)
pg_dump -h localhost -p 5432 -U postgres -d construction_db -F c -f construction_dump.dump

# Or if your local DB runs in Docker:
docker exec -it YOUR_LOCAL_DB_CONTAINER pg_dump -U postgres -d construction_db -F c -f /tmp/dump.dump
docker cp YOUR_LOCAL_DB_CONTAINER:/tmp/dump.dump ./construction_dump.dump
```

### 4b. Restore into the Docker database container

```bash
docker cp construction_dump.dump construction-db:/tmp/dump.dump
docker compose exec db pg_restore -U postgres -d construction_db -v --no-owner --no-acl /tmp/dump.dump
```

(Use a plain SQL dump with `psql` if `pg_restore` gives issues. For plain dump: `pg_dump ... -F p -f dump.sql`, then `docker cp dump.sql construction-db:/tmp/` and `docker compose exec db psql -U postgres -d construction_db -f /tmp/dump.sql`.)

### 4c. Create a superuser (optional)

```bash
docker compose exec backend python manage.py createsuperuser
```

---

## Step 5: Open the app

- In the browser: **http://localhost**
- Nginx (frontend) listens on port 80, serves static files, and forwards other requests to Django (backend).

---

## Summary of commands

| What to do | Command |
|------------|---------|
| Start all containers | `docker compose up -d --build` |
| Start only database | `docker compose up -d db` |
| Run migrations | `docker compose exec backend python manage.py migrate` |
| Create superuser | `docker compose exec backend python manage.py createsuperuser` |
| See logs | `docker compose logs -f` |
| Stop everything | `docker compose down` |
| Stop and remove data | `docker compose down -v` |

---

## Why no custom database image?

- The database image is `postgres:15`.
- Tables come from **Django migrations** (`manage.py migrate`), not from SQL in a custom image.
- Data is restored from a dump (Step 4), not baked into an image.

That keeps schema and data separate from the base image.

---

## Container responsibilities

| Container | Responsibility |
|-----------|----------------|
| **db** | PostgreSQL; stores all data. |
| **backend** | Django: views, API, models, migrations, collectstatic, Gunicorn. |
| **frontend** | Nginx: serves `/static/` and `/uploads/`, proxies rest to backend. |

---

## Updating the app

After code changes:

```bash
docker compose up -d --build backend
docker compose exec backend python manage.py migrate
```
