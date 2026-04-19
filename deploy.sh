#!/bin/bash
exec > /tmp/deploy.log 2>&1
echo "Starting deployment script detached from SSH..."

sudo killall dnf || true
sudo rm -f /var/lib/rpm/.rpm.lock

echo "Installing Git..."
sudo dnf install -y git

echo "Installing Docker..."
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo systemctl enable --now docker
sudo usermod -aG docker opc

echo "Removing old directory if it exists..."
rm -rf construction-cms

echo "Cloning repository..."
git clone https://github.com/NithiArr/Construction.git construction-cms
cd construction-cms

echo "Starting Docker Compose..."
sudo docker compose up -d --build

# Open Internal Firewall
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent || true
sudo firewall-cmd --reload || true

sudo iptables -I INPUT -m state --state NEW -p tcp --dport 80 -j ACCEPT || true
sudo netfilter-persistent save || true

echo "Deployment completed successfully!"
