# Migrate Local PostgreSQL to Supabase

This guide migrates your **local** Construction CMS PostgreSQL database (with your updated structure and data) into **Supabase** so Render can use it.

## Prerequisites

- Local PostgreSQL running (Docker Compose or local install) with your current data
- Supabase project created at [supabase.com](https://supabase.com)
- **Supabase database password** (from Project Settings → Database)
- **Direct connection string** from Supabase: Project Settings → Database → Connection string → **URI** (use "Session mode" / port **5432** for migration)

## Option A: Using the PowerShell script (Windows)

1. Get your Supabase connection string:
   - Supabase Dashboard → **Project Settings** → **Database**
   - Copy **Connection string** → **URI**
   - It looks like: `postgresql://postgres.[project-ref]:[YOUR-PASSWORD]@aws-0-[region].pooler.supabase.com:6543/postgres`
   - For migration you need the **direct** connection (not pooler). Use **Host** under "Direct connection" and port **5432**:
     - Host: `db.[project-ref].supabase.co`
     - Database: `postgres`
     - User: `postgres`
     - Password: your database password
   - Full direct URI: `postgresql://postgres:[YOUR-PASSWORD]@db.[project-ref].supabase.co:5432/postgres`

2. Ensure your local DB is running:
   - **Docker:** `docker compose up -d db`
   - **Local PostgreSQL:** ensure the service is running and you have `construction_db` with user `postgres` / password `admin123`

3. Run the migration script (from project root):
   ```powershell
   .\scripts\migrate_to_supabase.ps1 -SupabaseConnectionUri "postgresql://postgres:YOUR_PASSWORD@db.xxxx.supabase.co:5432/postgres"
   ```
   If your local DB is not on default (e.g. different port or Docker):
   ```powershell
   .\scripts\migrate_to_supabase.ps1 -SupabaseConnectionUri "postgresql://postgres:PASSWORD@db.xxx.supabase.co:5432/postgres" -LocalConnectionUri "postgresql://postgres:admin123@localhost:5432/construction_db"
   ```

4. The script will:
   - Dump your local `construction_db` (via Docker or local `pg_dump`)
   - Restore it into Supabase (direct connection, SSL)

5. Set `DATABASE_URL` on Render to your **Supabase** connection string (Session or Transaction pooler is fine for the app). Use the URI from Supabase that includes `?sslmode=require` or add `sslmode=require` for Django.

---

## Option B: Manual migration

### Step 1: Dump local database

**If using Docker Compose** (from project root):

```powershell
docker compose exec -T db pg_dump -U postgres -d construction_db -F c -f - > construction_dump.dump
```

**If using local PostgreSQL** (pg_dump in PATH):

```powershell
$env:PGPASSWORD = "admin123"
pg_dump -h localhost -p 5432 -U postgres -d construction_db -F c -f construction_dump.dump
```

- `-F c` = custom format (for `pg_restore`)
- Use `-F p` and `-f construction_dump.sql` if you prefer a plain SQL file and will use `psql` to restore.

### Step 2: Get Supabase connection details

- Supabase Dashboard → **Project Settings** → **Database**
- **Direct connection**: Host `db.xxxx.supabase.co`, Port **5432**, Database **postgres**, User **postgres**, Password = your DB password
- Build URI: `postgresql://postgres:YOUR_PASSWORD@db.xxxx.supabase.co:5432/postgres`

### Step 3: Restore into Supabase

**Using pg_restore** (if you used `-F c`):

```powershell
$env:PGPASSWORD = "YOUR_SUPABASE_PASSWORD"
pg_restore -h db.xxxx.supabase.co -p 5432 -U postgres -d postgres -v --no-owner --no-acl --clean --if-exists construction_dump.dump
```

- `--no-owner --no-acl` avoids permission errors with Supabase’s managed roles.
- `--clean --if-exists` drops existing objects before recreating (use only if Supabase DB is for this app and you want to replace everything).

If you see errors about “role postgres” or extensions, they are often safe to ignore as long as tables and data are created.

**Using psql** (if you used plain SQL dump):

```powershell
psql "postgresql://postgres:YOUR_PASSWORD@db.xxxx.supabase.co:5432/postgres?sslmode=require" -f construction_dump.sql
```

### Step 4: Configure Render

In Render dashboard for your service:

- **Environment** → add or set **DATABASE_URL** to your Supabase connection string.
- Use the **Connection pooling** URI (port 6543) for the app if you want; ensure it includes `?sslmode=require` (or equivalent) so Django uses SSL.

Example (replace with your project ref and password):

```
postgresql://postgres.[project-ref]:[PASSWORD]@aws-0-[region].pooler.supabase.com:6543/postgres?sslmode=require
```

Or direct:

```
postgresql://postgres:[PASSWORD]@db.[project-ref].supabase.co:5432/postgres?sslmode=require
```

Redeploy so the app uses the new database.

---

## Notes

- **Database name:** Supabase’s default database is `postgres`. Your local DB is `construction_db`; the dump contains only schema and data, so restoring into `postgres` is correct.
- **SSL:** Supabase requires SSL. The URI must use `sslmode=require` (or the Supabase-provided URI that already includes it).
- **Django migrations:** The dump includes `django_migrations`, so Supabase will match your local migration state; you don’t need to run `migrate` again for the same code.
- **Clean restore:** If Supabase already had tables from an older deploy, `pg_restore --clean --if-exists` replaces them. Back up anything you need from Supabase first.
