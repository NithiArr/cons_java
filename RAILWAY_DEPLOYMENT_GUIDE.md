# Railway Deployment Guide

Deploying this **Spring Boot** application to **Railway.app** is incredibly simple because Railway directly integrates with your GitHub repository and automatically manages the PostgreSQL database, Java build, and reverse proxy.

Follow these exact steps to successfully deploy the application on Railway.

---

### Step 1: Ensure your code is on GitHub
Railway pulls your code directly from a visual dashboard. Make sure your latest code is pushed to your GitHub repository.

### Step 2: Create a PostgreSQL Database on Railway
1. Go to [Railway.app](https://railway.app/) and log in with your GitHub account.
2. Click **New Project** -> **Provision PostgreSQL**.
3. Wait about 30 seconds for the database to spin up.

### Step 3: Deploy your Code
1. In the same project dashboard, click the **+ Create** button (or **+ New**).
2. Select **GitHub Repo** and choose your repository.
3. Railway will start cloning the repo.
4. **Important**: Wait for the first build. It might fail because it doesn't know about the database yet, which is fine.

### Step 4: Configure the Deployment Settings
By default, Railway can detect Maven and build it via Nixpacks, but we already have a specialized `Dockerfile` that optimizes the build process.

1. Click on your newly created web service box in the Railway dashboard.
2. Go to the **Settings** tab.
3. Scroll down to the **Build** section:
   - Change the **Builder** to `Dockerfile`.
   - Set the **Dockerfile Path** to `/Dockerfile` (if it isn't already).
4. Scroll down to the **Deploy** section:
   - Under **Start Command**, leave it blank (it will use the `ENTRYPOINT` from our Dockerfile).
   - Under **Networking** / **Custom Domain**, click **Generate Domain** to get a free `x.up.railway.app` URL for your site.

### Step 5: Configure Environment Variables
Railway needs the same secret variables you use locally, plus a connection to the newly created database.

1. Still on your web service, go to the **Variables** tab.
2. Click **New Variable** -> **Add Reference** -> Select the `DATABASE_URL` from your PostgreSQL plugin.
   *(This actually automatically brings in `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, and `PGDATABASE` under the hood! Our `application.yml` is already configured to automatically use these `PG*` variables when running on Railway).*
3. Add the following additional variable manually:
   - `PORT`: `8080` *(Very Important: Our `Dockerfile` exposes port 8080, and Railway must be told to map its public traffic to this port inside the container).*

### Step 6: Wait for the Build and Verify
Whenever you update settings or variables, Railway triggers a new deployment.
Wait for it to show a green **Success** badge.

Because Spring Boot is configured with `spring.jpa.hibernate.ddl-auto=update`, the application will automatically create all your database tables upon startup!

**That's it!** You can now visit the custom URL generated in Step 4, and your app will be live with full SSL and a connected database.

---
### *Need to Restore Data?*
If you want to move your local data to Railway:
1. Railway explicitly provides the PostgreSQL connection details (Host, Port, User, Password, DB Name) in the Database service's "Variables" tab.
2. Run this command on your **local computer**, replacing the caps with the Railway database variables:
```bash
pg_restore -U PGUSER -h PGHOST -p PGPORT -d PGDATABASE -v --no-owner --no-acl construction_dump.dump
```
