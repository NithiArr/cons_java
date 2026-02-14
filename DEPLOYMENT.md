# Deployment Guide (Local App + Cloud Database)

Since we are skipping the full GCP deployment for now, here is how to run your app locally with the **MongoDB Atlas** cloud database.

## 1. Prerequisites
- **Python 3.11+** installed.
- **Git** installed.
- **MongoDB Atlas Connection String** (already in your `.env`).

## 2. Run the Application
Simply run the following command in your terminal:

```bash
python app.py
```

Your app will start at `http://localhost:5000`.

## 3. Data Persistence
All your data is now saved in the **cloud** (MongoDB Atlas).
- You can close the app and restart it; your data will still be there.
## 4. (Optional) Migrate Local Data
If you have data in your local Docker container that you want to move to the cloud:

1.  Make sure your local MongoDB is running:
    ```bash
    docker-compose up -d mongodb
    ```
2.  Run the migration script:
    ```bash
    python migrate_mongo_local_to_cloud.py
    ```
3.  This will copy all users, projects, and expenses to MongoDB Atlas.

---

## Step 1: Create a VM Instance

1.  Go to the **Google Cloud Console** > **Compute Engine** > **VM instances**.
2.  Click **Create Instance**.
3.  **Name**: `construction-app-server` (or similar).
4.  **Region**: Choose a region close to you (e.g., `asia-south1` for Mumbai).
5.  **Machine Type**: `e2-medium` (2 vCPU, 4GB RAM) is recommended. `e2-micro` might be too small for MongoDB + Flask.
6.  **Boot Disk**:
    -   OS: **Ubuntu** (22.04 LTS or 24.04 LTS).
    -   Size: **20 GB** (Standard Persistent Disk).
7.  **Firewall**: Check both **Allow HTTP traffic** and **Allow HTTPS traffic**.
8.  Click **Create**.

---

## Step 2: Configure Firewall Rules

By default, GCP allows HTTP (80) and HTTPS (443). If you want to access the app on a custom port (like 8080), you need a custom firewall rule.
**However**, our `docker-compose.prod.yml` maps port 80 to 8080, so the default HTTP rule is sufficient!

---

## Step 3: Connect to the VM

1.  In the VM instances list, click **SSH** next to your new instance.
2.  A terminal window will open in your browser.

---

## Step 4: Install Docker & Git

Run the following commands in the SSH terminal:

```bash
# Update packages
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Set up the repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Verify Docker Compose
sudo docker compose version
```

---

## Step 5: Clone & Configure Your App

1.  **Clone your repository**:
    ```bash
    git clone <YOUR_GITHUB_REPO_URL> construction-app
    cd construction-app
    ```
    *(If you haven't pushed your code to GitHub yet, you'll need to do that first!)*

2.  **Create your Environment File**:
    ```bash
    nano .env
    ```
    Paste your production environment variables:
    ```
    SECRET_KEY=your_secure_random_key_here
    MONGO_INITDB_ROOT_USERNAME=admin
    MONGO_INITDB_ROOT_PASSWORD=secure_password_here
    # ... other variables
    ```
    Press `Ctrl+X`, then `Y`, then `Enter` to save.

3.  **Make Deploy Script Executable**:
    ```bash
    chmod +x deploy.sh
    ```

---

## Step 6: Deploy!

Run the deployment script:

```bash
sudo ./deploy.sh
```

This will:
1.  Pull the latest code from GitHub.
2.  Build the Docker images.
3.  Start the containers in the background.

---

## Step 7: Access Your App

Open your browser and visit:
`http://<YOUR_VM_EXTERNAL_IP>`

You should see the login page!

---

## Troubleshooting

-   **Logs**: Check app logs with:
    ```bash
    sudo docker compose -f docker-compose.prod.yml logs -f app
    ```
-   **Restart**:
    ```bash
    sudo docker compose -f docker-compose.prod.yml restart app
    ```
-   **Updates**: To update the app later, just run `./deploy.sh` again.
