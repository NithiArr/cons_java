#!/bin/bash
echo "Configuring firewalld..."
sudo firewall-cmd --zone=public --add-port=80/tcp --permanent || true
sudo firewall-cmd --reload || true

echo "Configuring iptables (fallback)..."
sudo iptables -I INPUT -m state --state NEW -p tcp --dport 80 -j ACCEPT || true
sudo netfilter-persistent save || true

echo "Done installing firewall rules."
