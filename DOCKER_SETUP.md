# Docker Setup – Step by Step (Java version)

Your app runs in **2 separate containers**:

| Container | Role | Image / Build |
|-----------|------|---------------|
| **db** | PostgreSQL database | `postgres:15` (official) |
| **app** | Spring Boot (Java) app | Built from `Dockerfile` |

Traffic flow: **Browser → Host (port 80) → Container (port 8080)**.

---

## Step 1: Start the database container (optional, if you want database only)

The database uses the official PostgreSQL image. 

```bash
docker compose up -d db
```

What happens: PostgreSQL 15 starts in a container named `construction-db`, creates the database `construction_db`, and stores data in the `postgres_data` volume.

---

## Step 2: Build and start both containers

To build the Spring Boot application and start both the application and database containers:

```bash
docker compose up -d --build
```

This will:
1. **db**: Start PostgreSQL database.
2. **app**: Compile the Java application using Maven (multi-stage build in `Dockerfile`) and run the Spring Boot application.

---

## Step 3: Database Tables & Migrations

The database tables are automatically managed by Hibernate (`ddl-auto: update` in `application.yml`).
When the Spring Boot application starts, it will automatically connect to the database container and create all the necessary schema tables.

---

## Step 4: Import existing data into the database container

If you have a SQL dump or file backup (e.g., `construction_data.sql` or `backup.sql`) and want to restore it into the docker container:

```bash
# Copy the SQL file into the database container
docker cp your_backup.sql construction-db:/tmp/backup.sql

# Execute psql to restore the database
docker compose exec db psql -U postgres -d construction_db -f /tmp/backup.sql
```

---

## Step 5: Open the app

- In the browser: **http://localhost**
- The host port 80 maps directly to port 8080 of the Spring Boot application.

---

## Summary of commands

| What to do | Command |
|------------|---------|
| Start all containers | `docker compose up -d --build` |
| Start only database | `docker compose up -d db` |
| See logs | `docker compose logs -f` |
| Stop everything | `docker compose down` |
| Stop and remove data | `docker compose down -v` |

---

## Container responsibilities

| Container | Responsibility |
|-----------|----------------|
| **db** | PostgreSQL; stores all data. |
| **app** | Spring Boot: controllers, service layer, Thymeleaf rendering, and direct database access. |
