# Deploy Construction CMS on Google Cloud (Single VM)

This guide walks you through hosting **both the app and PostgreSQL** on one Google Cloud Compute Engine VM. Itâ€™s cost-effective and keeps everything in one place.

---

## Overview

| Component   | Container   | Where it runs          |
|-------------|-------------|------------------------|
| Database    | `db`        | PostgreSQL 15          |
| Backend     | `backend`   | Django (Gunicorn)      |
| Frontend    | `frontend`  | Nginx (static + proxy) |

**You will:** create a VM â†’ install Docker â†’ run the project with Docker Compose â†’ open HTTP to the internet.

---

## 1. Prerequisites

- Google account
- [Google Cloud SDK (gcloud)](https://cloud.google.com/sdk/docs/install) installed (optional; you can do everything in the Cloud Console)

---

## 2. Using the $300 free credit (new accounts)

Google Cloud offers **$300 free credit for 90 days** for new customers. You can use it to run this app without paying until the credit is used.

**How to get it**
- Go to [Google Cloud Console](https://console.cloud.google.com/) and sign in.
- If you're eligible, you'll be prompted to start the **Free Trial** and redeem the $300 credit.
- You must add a **billing account** (card required). Google will not charge you until the $300 is used or the 90 days end; you can then turn off billing or upgrade.

**Keep costs low so the credit lasts**
- **VM:** Use **e2-small** (0.5 GB RAM) or **e2-medium** (1 GB). Avoid larger machines at first.
- **Disk:** **20 GB** standard persistent disk is enough; avoid SSD or larger disks unless needed.
- **Region:** Pick one close to you (e.g. `us-central1`, `asia-south1`). Prices vary slightly by region.
- **No extra services:** For this guide you only need **Compute Engine** (the VM). Don't enable Cloud SQL, Cloud Run, or other paid APIs unless you plan to use them.

**Rough cost (for planning)**
- **e2-small** + 20 GB disk: about **$15–25/month** (varies by region).
- **e2-medium** + 20 GB disk: about **$30–45/month**.
- So **$300** can typically run an **e2-small** setup for most or all of the 90-day trial.

**Set a budget alert (recommended)**
- **Billing** → **Budgets & alerts** → **Create budget**.
- Set a limit (e.g. $50 or $100) and add an email alert at 50% and 90%. That way you won't burn through the credit by surprise.

---

## 3. Create a GCP project and enable billing

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project or select one: **Select a project** â†’ **New Project** (e.g. `construction-cms-prod`).
3. Enable billing: **Billing** â†’ link a billing account to this project.
4. Enable Compute Engine: **APIs & Services** â†’ **Enable APIs and Services** â†’ search **Compute Engine API** â†’ **Enable**.

---

## 4. Create the VM

1. Open **Compute Engine** â†’ **VM instances**.
2. Click **Create Instance**.
3. Set:
   - **Name:** e.g. `construction-cms`
   - **Region:** e.g. `us-central1` (or your preferred region).
   - **Machine type:** `e2-small` (0.5 GB memory) or `e2-medium` (1 GB). For more traffic, use `e2-medium` or `e2-standard-2`.
   - **Boot disk:** **Change** â†’ **Ubuntu** (e.g. Ubuntu 22.04 LTS) â†’ **20 GB** or more.
   - **Firewall:** enable **Allow HTTP traffic** and **Allow HTTPS traffic** (so the app can be reached on ports 80/443).
4. Click **Create**. Note the **External IP** (e.g. `34.x.x.x`).

---

## 5. Firewall

The production Compose file maps **port 80** on the VM to the app. With **Allow HTTP traffic** enabled in step 4, no extra firewall rule is needed. If you expose port 8000 instead, add a rule for **TCP** port **8000** from `0.0.0.0/0`.

Alternatively, you can change the app to listen on port **80** (see â€œRun on port 80â€ in the script section) 
---

## 6. Connect to the VM and install Docker

1. In **VM instances**, click **SSH** next to your instance (or use your own SSH key).
2. In the SSH terminal, run:

```bash
# Update system
sudo apt-get update && sudo apt-get upgrade -y

# Install Docker
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow your user to run Docker (optional; or use sudo for docker commands)
sudo usermod -aG docker $USER
# Log out and back in for this to take effect, or run the next commands with sudo
```

---

## 7. Deploy the app on the VM

### Option A: Clone from Git (recommended)

```bash
# Clone (replace with your repo URL)
sudo git clone https://github.com/YOUR_USERNAME/Construction.git /opt/construction
cd /opt/construction
```

If the repo is private, use a deploy key or personal access token in the URL.

### Option B: Upload code with gcloud

From your **local machine** (with gcloud and project set):

```bash
gcloud compute scp --recurse ./Construction VM_NAME:/opt/construction --zone=ZONE
# Example: gcloud compute scp --recurse . construction-cms:/opt/construction --zone=us-central1-a
```

Then on the VM: `cd /opt/construction`.

---

## 7A. (Optional) Build image and push to Artifact Registry

To run **without cloning code on the VM**: build the backend image locally, push to Artifact Registry, then on the VM use a compose file that references `image:` instead of `build:`. This requires creating a custom compose file and is documented in the project’s image-based deployment option.

### 7A.1 One-time: Create Artifact Registry

1. In Google Cloud Console: **Artifact Registry** → **Create repository**.
2. **Name:** e.g. `construction-cms`. **Format:** Docker. **Mode:** Standard. **Region:** same as your VM (e.g. `us-central1`).
3. Click **Create**.
4. Enable the API if prompted: **APIs & Services** → enable **Artifact Registry API**.

Note your **image path** (e.g. `us-central1-docker.pkg.dev/YOUR_PROJECT_ID/construction-cms/construction-cms:latest`). You will use it as `APP_IMAGE` below.

### 7A.2 Build and push the app image (from your local machine)

On your **local machine** (where you have the project and Docker):

```bash
# Replace REGION, PROJECT_ID, REPO_NAME with your values (e.g. us-central1, my-project, construction-cms)
export REGION=us-central1
export PROJECT_ID=your-gcp-project-id
export REPO_NAME=construction-cms
export IMAGE=${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO_NAME}/construction-cms:latest

# Log in Docker to Artifact Registry
gcloud auth configure-docker ${REGION}-docker.pkg.dev --quiet

# Build the image
docker build -t $IMAGE .

# Push the image
docker push $IMAGE
```

The **database** is not a custom image; on the VM you will use the standard `postgres:15` image from Docker Hub. Only the **app** image is built and pushed to Artifact Registry.

### 7A.3 On the VM: run using the image (no source code)

You need the **compose file** and **.env** on the VM. No need to clone the full repo.

1. **SSH into the VM** and create a directory:

```bash
sudo mkdir -p /opt/construction
cd /opt/construction
```

2. **Create `.env`** (generate strong values for SECRET_KEY and POSTGRES_PASSWORD):

```bash
sudo nano .env
```

Put (replace with your real values and your image path from 7A.1):

```env
SECRET_KEY=your-generated-secret-key
POSTGRES_PASSWORD=your-generated-db-password
APP_IMAGE=us-central1-docker.pkg.dev/YOUR_PROJECT_ID/construction-cms/construction-cms:latest
DEBUG=False
```

3. **Copy the image-based compose file** to the VM. From your **local machine** (in the project directory):

```bash
gcloud compute scp docker-compose.prod.image.yml VM_NAME:/opt/construction/ --zone=ZONE
# Example: gcloud compute scp docker-compose.prod.image.yml construction-cms:/opt/construction/ --zone=us-central1-a
```

Or on the VM, create the file manually (copy contents of `docker-compose.prod.image.yml`).

4. **Let the VM pull from Artifact Registry.** Either:

- **Option A:** Install gcloud on the VM and run:  
  `gcloud auth configure-docker REGION-docker.pkg.dev --quiet`  
  (Use the same Google account that can read the registry.)

- **Option B:** Attach a **service account** to the VM that has the **Artifact Registry Reader** role. Then no `gcloud auth` on the VM; Docker will use the VM’s service account to pull.

5. **Run the stack** (on the VM):

```bash
cd /opt/construction
sudo docker compose -f docker-compose.prod.image.yml --env-file .env pull
sudo docker compose -f docker-compose.prod.image.yml --env-file .env up -d
```

6. **Migrations and superuser** (one-time):

```bash
sudo docker compose -f docker-compose.prod.image.yml exec backend python manage.py migrate --noinput
sudo docker compose -f docker-compose.prod.image.yml exec backend python manage.py createsuperuser
```

After this, the app and database both run from images: **app** from Artifact Registry, **database** from `postgres:15`. To update the app, build and push a new image tag, then on the VM run `docker compose pull` and `docker compose up -d` (and `migrate` if needed).

---

## 8. Set production environment variables

On the VM, create an env file (do **not** commit this file; keep it only on the server):

```bash
cd /opt/construction

# Generate a strong SECRET_KEY and password
python3 -c "import secrets; print('SECRET_KEY=', secrets.token_urlsafe(50))"
python3 -c "import secrets; print('POSTGRES_PASSWORD=', secrets.token_urlsafe(24))"
```

Create `.env` (replace the placeholders with the values you generated):

```bash
sudo nano .env
```

Put this in `.env` (use your own values):

```env
SECRET_KEY=your-generated-secret-key-from-above
POSTGRES_PASSWORD=your-generated-db-password
DEBUG=False
```

The compose file reads `SECRET_KEY` and `POSTGRES_PASSWORD` from `.env`. Do **not** commit `.env` to Git.

---

## 9. Run with Docker Compose

Uses your `.env` for `SECRET_KEY` and `POSTGRES_PASSWORD`. If `.env` is missing, defaults are used (fine for local testing only).

```bash
cd /opt/construction
sudo docker compose -f docker-compose.yml --env-file .env up -d --build
```

Wait for the build and startup. Then:

```bash
# Run migrations (one-time, or after pulling new code)
sudo docker compose -f docker-compose.yml exec backend python manage.py migrate --noinput

# Create a superuser (optional)
sudo docker compose -f docker-compose.yml exec backend python manage.py createsuperuser
```

---

## 10. Check that the app is running

- From the VM: `curl -s -o /dev/null -w "%{http_code}" http://localhost` (should be 200 or 302).
- From your browser: `http://YOUR_VM_EXTERNAL_IP` (e.g. `http://34.x.x.x`). Nginx (frontend) listens on port 80.

---

## 11. Port 80

Your setup already uses port 80: the **frontend** (Nginx) container maps `80:80`, so users open `http://YOUR_VM_EXTERNAL_IP` with no port number.

---

## 12. Auto-start on reboot (systemd)

Create a systemd unit so the stack starts after a VM reboot:

```bash
sudo tee /etc/systemd/system/construction-cms.service << 'EOF'
[Unit]
Description=Construction CMS Docker Compose
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/construction
ExecStart=/usr/bin/docker compose -f docker-compose.yml --env-file .env up -d
ExecStop=/usr/bin/docker compose -f docker-compose.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable construction-cms
```

After reboots, the app and DB will start automatically. To start/stop manually:

```bash
sudo systemctl start construction-cms
sudo systemctl stop construction-cms
```

---

## 13. Backups (database)

- **VM disk snapshots:** In **Compute Engine** â†’ **Disks** â†’ select the VMâ€™s disk â†’ **Create snapshot**. Schedule snapshots (e.g. daily) for full disk backup.
- **PostgreSQL dump:** Add a cron job on the VM (run from the project directory so `docker compose` finds the stack):

```bash
sudo mkdir -p /opt/construction/backups
sudo crontab -e
```

Add this line (daily at 2 AM; the `-T` avoids allocating a TTY):

```cron
0 2 * * * cd /opt/construction && /usr/bin/docker compose -f docker-compose.yml exec -T db pg_dump -U postgres construction_db | gzip > /opt/construction/backups/db_$(date +\%Y\%m\%d).sql.gz
```

---

## 14. Updates (after code changes)

```bash
cd /opt/construction
sudo git pull   # if using Git
sudo docker compose -f docker-compose.yml --env-file .env up -d --build
sudo docker compose -f docker-compose.yml exec backend python manage.py migrate --noinput
sudo docker compose -f docker-compose.yml exec backend python manage.py collectstatic --noinput
```

---

## 15. Optional: custom domain and HTTPS

1. Point your domainâ€™s A record to the VMâ€™s external IP.
2. Install Nginx and Certbot on the VM, or run a container that does reverse proxy + Letâ€™s Encrypt (e.g. Caddy or nginx-proxy with certbot).
3. Proxy `https://yourdomain.com` â†’ `http://localhost:8000` (or 80).
4. In Django `settings.py`, set `ALLOWED_HOSTS = ['yourdomain.com', 'www.yourdomain.com']` (and remove `'*'` in production if you want).

---

## Quick reference

| Task              | Command |
|-------------------|--------|
| View logs         | `sudo docker compose -f docker-compose.yml logs -f` |
| Restart app       | `sudo docker compose -f docker-compose.yml restart backend` |
| Shell in app      | `sudo docker compose -f docker-compose.yml exec backend bash` |
| Django manage.py  | `sudo docker compose -f docker-compose.yml exec backend python manage.py ...` |
| Stop everything   | `sudo docker compose -f docker-compose.yml down` |

---

## Cloud Run + Cloud SQL (alternative)

If you prefer **not** to manage a VM, you can run the app on **Cloud Run** and the database on **Cloud SQL**. The beginning of this repoâ€™s GCP guide (sections on Cloud Run, Artifact Registry, Cloud SQL) still applies; use that for a fully managed option.

For a **single VM for both app and database**, the steps above are sufficient.
