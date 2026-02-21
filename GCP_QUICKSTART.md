# GCP Quick Start – Deploy with $300 Free Credit

Use your Docker setup (db + backend + frontend) on a single GCP VM and stay within the free $300 credit.

---

## 1. Get the $300 credit

- Go to [Google Cloud Console](https://console.cloud.google.com/), sign in.
- Start the **Free Trial** and add a billing account (card required).
- Create a **project** (e.g. `construction-cms`).
- Enable **Compute Engine API**: APIs & Services → Enable APIs → search **Compute Engine API** → Enable.
- Optional: set a budget alert (e.g. $50) so you know when you’re using credit.

---

## 2. Create the VM

1. **Compute Engine** → **VM instances** → **Create Instance**.
2. Settings:
   - **Name:** `construction-cms`
   - **Region:** `us-central1` or `asia-south1`
   - **Machine type:** `e2-small` (or `e2-medium`)
   - **Boot disk:** Ubuntu 22.04 LTS, **20 GB**
   - **Firewall:** enable **Allow HTTP traffic**
3. Create and note the **External IP**.

---

## 3. Install Docker on the VM

1. Click **SSH** next to your instance.
2. Run:

```bash
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

---

## 4. Put your code on the VM

**Option A – Git (if repo is on GitHub):**

```bash
sudo git clone https://github.com/YOUR_USERNAME/Construction.git /opt/construction
cd /opt/construction
```

**Option B – Upload from your machine:**

```bash
# From your local machine (in the Construction folder)
gcloud compute scp --recurse . construction-cms:/opt/construction --zone=us-central1-a
```

On the VM: `cd /opt/construction`.

---

## 5. Create `.env` for production

```bash
cd /opt/construction
python3 -c "import secrets; print('SECRET_KEY=' + secrets.token_urlsafe(50))"
python3 -c "import secrets; print('POSTGRES_PASSWORD=' + secrets.token_urlsafe(24))"
```

Create `.env`:

```bash
sudo nano .env
```

Contents (use the values you generated):

```env
SECRET_KEY=your-generated-secret-key
POSTGRES_PASSWORD=your-generated-db-password
DEBUG=False
```

---

## 6. Run the app

```bash
cd /opt/construction
sudo docker compose --env-file .env up -d --build
```

---

## 7. Migrations and data

```bash
# Create tables
sudo docker compose exec backend python manage.py migrate

# Create admin user
sudo docker compose exec backend python manage.py createsuperuser
```

**Migrate local data (optional):**

1. On your machine: `pg_dump -h localhost -U postgres -d construction_db -F c -f dump.dump`
2. Upload and restore:
   ```bash
   gcloud compute scp dump.dump construction-cms:/tmp/ --zone=us-central1-a
   ```
3. On the VM:
   ```bash
   docker cp /tmp/dump.dump construction-db:/tmp/
   sudo docker compose exec db pg_restore -U postgres -d construction_db -v --no-owner --no-acl /tmp/dump.dump
   ```

---

## 8. Open the app

- In a browser: `http://YOUR_VM_EXTERNAL_IP`

---

## Summary

| Step | Action |
|------|--------|
| 1 | Get $300 credit, create project, enable Compute Engine |
| 2 | Create VM (e2-small, Ubuntu, allow HTTP) |
| 3 | Install Docker on VM |
| 4 | Clone or upload code to `/opt/construction` |
| 5 | Create `.env` with SECRET_KEY and POSTGRES_PASSWORD |
| 6 | `docker compose --env-file .env up -d --build` |
| 7 | `migrate` and `createsuperuser` (and optional data restore) |
| 8 | Visit `http://YOUR_VM_IP` |

For more detail, see `GCP_DEPLOYMENT_GUIDE.md`.
