# CrisisConnect Production Deployment Guide

> **Last Updated:** 2026-01-23

This guide provides comprehensive instructions for deploying CrisisConnect in a production environment.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Prerequisites](#prerequisites)
3. [Deployment Options](#deployment-options)
4. [Configuration](#configuration)
5. [Security Hardening](#security-hardening)
6. [Database Setup](#database-setup)
7. [Docker Deployment](#docker-deployment)
8. [Manual Deployment](#manual-deployment)
9. [Monitoring & Maintenance](#monitoring--maintenance)
10. [Troubleshooting](#troubleshooting)

---

## Quick Start

For production deployment using Docker:

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/CrisisConnect.git
cd CrisisConnect

# 2. Configure environment
cp .env.example .env
nano .env  # Edit with secure production values

# 3. Deploy
docker-compose up -d

# 4. Verify
curl http://localhost:8080/api/actuator/health
```

---

## Prerequisites

### Required Software

- **Docker** 20.10+ and **Docker Compose** 2.0+
- **OR** for manual deployment:
  - Java 17 (OpenJDK or Oracle JDK)
  - Node.js 18+
  - PostgreSQL 15+
  - Nginx (for frontend serving)

### System Requirements

**Minimum (Small deployment, <100 users):**
- 2 CPU cores
- 4 GB RAM
- 20 GB disk space
- 1 Mbps network

**Recommended (Production, 100-1000 users):**
- 4 CPU cores
- 8 GB RAM
- 50 GB disk space (with room for logs and backups)
- 10 Mbps network

**Enterprise (1000+ users):**
- 8+ CPU cores
- 16+ GB RAM
- 100+ GB disk space
- Load balancer
- Database replication

---

## Deployment Options

### Option 1: Docker Compose (Recommended)

**Best for:** Quick deployment, easier management, isolated environments

```bash
docker-compose up -d
```

### Option 2: Docker Swarm

**Best for:** Multi-node deployments, high availability

```bash
docker stack deploy -c docker-compose.yml crisisconnect
```

### Option 3: Kubernetes

**Best for:** Large-scale deployments, auto-scaling, enterprise environments

See `k8s/` directory for Kubernetes manifests (if available).

### Option 4: Manual Deployment

**Best for:** Custom infrastructure, specific requirements

See [Manual Deployment](#manual-deployment) section below.

---

## Configuration

### Environment Variables

Create `.env` file from template:

```bash
cp .env.example .env
```

### Critical Security Variables

**MUST BE CHANGED FOR PRODUCTION:**

```env
# Database Configuration
DB_PASSWORD=CHANGE_TO_SECURE_PASSWORD_32_CHARS_MIN

# JWT Configuration
JWT_SECRET=CHANGE_TO_CRYPTOGRAPHICALLY_SECURE_RANDOM_STRING_32_CHARS_MIN

# Encryption Configuration (CRITICAL!)
ENCRYPTION_SECRET=CHANGE_TO_32_BYTE_HEX_STRING

# Admin Account
ADMIN_PASSWORD=CHANGE_TO_NIST_COMPLIANT_PASSWORD
ADMIN_BOOTSTRAP_ENABLED=true  # Set to false after first admin creation

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### Generate Secure Secrets

```bash
# Generate JWT_SECRET (64 characters)
openssl rand -base64 48

# Generate ENCRYPTION_SECRET (32 bytes = 64 hex characters)
openssl rand -hex 32

# Generate secure admin password
openssl rand -base64 24
```

### Complete Environment Configuration

```env
# ========================================
# DATABASE CONFIGURATION
# ========================================
DB_HOST=db
DB_PORT=5432
DB_NAME=crisisconnect
DB_USER=crisisconnect
DB_PASSWORD=YOUR_SECURE_DATABASE_PASSWORD_HERE

# ========================================
# SECURITY CONFIGURATION
# ========================================
# JWT Secret (minimum 32 characters, recommend 64+)
JWT_SECRET=YOUR_CRYPTOGRAPHICALLY_SECURE_JWT_SECRET_HERE

# AES-256 Encryption Key (exactly 64 hex characters = 32 bytes)
ENCRYPTION_SECRET=YOUR_32_BYTE_ENCRYPTION_KEY_IN_HEX_HERE

# ========================================
# ADMIN BOOTSTRAP (Disable after first login!)
# ========================================
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=admin@yourorg.org
ADMIN_PASSWORD=YOUR_NIST_COMPLIANT_ADMIN_PASSWORD
ADMIN_NAME=System Administrator

# ========================================
# CORS CONFIGURATION
# ========================================
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# ========================================
# APPLICATION URLS
# ========================================
REACT_APP_API_URL=https://yourdomain.com/api
```

---

## Security Hardening

### 1. Change Default Credentials

**IMMEDIATELY after deployment:**

```bash
# 1. Login with bootstrap admin credentials
# 2. Create a new admin user with secure password
# 3. Set ADMIN_BOOTSTRAP_ENABLED=false
# 4. Restart the application
```

### 2. Enable HTTPS/TLS

**Using Nginx reverse proxy:**

```bash
# Install certbot
sudo apt-get install certbot python3-certbot-nginx

# Obtain SSL certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal is configured automatically
```

**Sample Nginx configuration:**

```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # Modern SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

### 3. Database Security

```bash
# PostgreSQL hardening

# 1. Use strong password
ALTER USER crisisconnect WITH PASSWORD 'very_secure_password_here';

# 2. Restrict connections (postgresql.conf)
listen_addresses = 'localhost'

# 3. Enable SSL (postgresql.conf)
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'

# 4. Configure pg_hba.conf
hostssl    crisisconnect    crisisconnect    127.0.0.1/32    scram-sha-256
```

### 4. Firewall Configuration

```bash
# UFW (Ubuntu)
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw enable

# Block direct access to backend port
sudo ufw deny 8080/tcp

# iptables alternative
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP
```

### 5. Rate Limiting

The application includes built-in rate limiting:
- Login attempts: 5 per 15 minutes per IP
- API requests: Configurable per endpoint

Additional protection with Nginx:

```nginx
# In http block
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

# In location /api/ block
limit_req zone=api burst=20 nodelay;
```

---

## Database Setup

### PostgreSQL Installation

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install postgresql-15

# CentOS/RHEL
sudo yum install postgresql15-server
sudo postgresql-15-setup initdb
sudo systemctl enable --now postgresql-15
```

### Create Database

```bash
sudo -u postgres psql

CREATE DATABASE crisisconnect;
CREATE USER crisisconnect WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE crisisconnect TO crisisconnect;

# PostgreSQL 15+ also requires:
\c crisisconnect
GRANT ALL ON SCHEMA public TO crisisconnect;

\q
```

### Database Backups

**Automated backup script:**

```bash
#!/bin/bash
# /usr/local/bin/backup-crisisconnect-db.sh

BACKUP_DIR="/var/backups/crisisconnect"
DB_NAME="crisisconnect"
DB_USER="crisisconnect"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/crisisconnect_$TIMESTAMP.sql.gz"

# Create backup
PGPASSWORD="$DB_PASSWORD" pg_dump -h localhost -U $DB_USER $DB_NAME | gzip > $BACKUP_FILE

# Encrypt backup
gpg --encrypt --recipient admin@yourorg.org $BACKUP_FILE

# Remove old backups (keep 30 days)
find $BACKUP_DIR -name "*.sql.gz.gpg" -mtime +30 -delete

# Upload to S3 (optional)
aws s3 cp $BACKUP_FILE.gpg s3://your-backup-bucket/crisisconnect/
```

**Cron job (daily at 2 AM):**

```bash
0 2 * * * /usr/local/bin/backup-crisisconnect-db.sh
```

---

## Docker Deployment

### Full Production Deployment

```bash
# 1. Build and start all services
docker-compose up -d

# 2. Check logs
docker-compose logs -f backend

# 3. Verify health
curl http://localhost:8080/api/actuator/health

# 4. Access application
# Frontend: http://localhost (port 80)
# Backend: http://localhost:8080/api
```

### Docker Compose with Custom Network

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  db:
    image: postgres:15-alpine
    restart: always
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - db_data:/var/lib/postgresql/data
      - ./backups:/backups
    networks:
      - backend-network

  backend:
    build: ./backend
    restart: always
    env_file: .env
    depends_on:
      - db
    networks:
      - backend-network
      - frontend-network

  frontend:
    build:
      context: ./frontend
      args:
        REACT_APP_API_URL: ${REACT_APP_API_URL}
    restart: always
    depends_on:
      - backend
    networks:
      - frontend-network

  nginx:
    image: nginx:alpine
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - frontend
      - backend
    networks:
      - frontend-network

volumes:
  db_data:

networks:
  backend-network:
    driver: bridge
  frontend-network:
    driver: bridge
```

### Updating the Application

```bash
# 1. Pull latest changes
git pull origin main

# 2. Rebuild containers
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# 3. Verify
docker-compose ps
docker-compose logs -f backend
```

---

## Manual Deployment

### Backend Deployment

```bash
# 1. Build the application
cd backend
mvn clean package -DskipTests

# 2. Copy JAR to deployment directory
sudo mkdir -p /opt/crisisconnect
sudo cp target/crisisconnect-backend-*.jar /opt/crisisconnect/app.jar

# 3. Create systemd service
sudo nano /etc/systemd/system/crisisconnect-backend.service
```

**Systemd service file:**

```ini
[Unit]
Description=CrisisConnect Backend
After=postgresql.service
Requires=postgresql.service

[Service]
Type=simple
User=crisisconnect
Group=crisisconnect
WorkingDirectory=/opt/crisisconnect

Environment="DB_HOST=localhost"
Environment="DB_PORT=5432"
Environment="DB_NAME=crisisconnect"
Environment="DB_USER=crisisconnect"
EnvironmentFile=/opt/crisisconnect/.env

ExecStart=/usr/bin/java -Xmx2g -Xms1g -jar /opt/crisisconnect/app.jar

Restart=always
RestartSec=10

StandardOutput=journal
StandardError=journal
SyslogIdentifier=crisisconnect-backend

[Install]
WantedBy=multi-user.target
```

```bash
# 4. Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable crisisconnect-backend
sudo systemctl start crisisconnect-backend

# 5. Check status
sudo systemctl status crisisconnect-backend
sudo journalctl -u crisisconnect-backend -f
```

### Frontend Deployment

```bash
# 1. Build the frontend
cd frontend
npm install
npm run build

# 2. Copy build to nginx
sudo cp -r build/* /var/www/crisisconnect/

# 3. Configure nginx (see Security Hardening section)
sudo nano /etc/nginx/sites-available/crisisconnect

# 4. Enable site
sudo ln -s /etc/nginx/sites-available/crisisconnect /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## Monitoring & Maintenance

### Application Monitoring

```bash
# View backend logs
docker-compose logs -f backend

# Or with systemd
sudo journalctl -u crisisconnect-backend -f

# Check application health
curl http://localhost:8080/api/actuator/health
```

### Database Monitoring

```sql
-- Active connections
SELECT count(*) FROM pg_stat_activity;

-- Database size
SELECT pg_size_pretty(pg_database_size('crisisconnect'));

-- Table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Log Rotation

```bash
# /etc/logrotate.d/crisisconnect
/var/log/crisisconnect/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 640 crisisconnect crisisconnect
    sharedscripts
    postrotate
        systemctl reload crisisconnect-backend > /dev/null 2>&1 || true
    endscript
}
```

### Performance Tuning

**Java JVM Options:**

```bash
# In systemd service or docker-compose
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**Database Connection Pool:**

```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
docker-compose logs backend
# or
sudo journalctl -u crisisconnect-backend -n 100

# Common issues:
# 1. Database not reachable
# 2. Invalid JWT_SECRET or ENCRYPTION_SECRET
# 3. Port 8080 already in use
# 4. Insufficient memory
```

### Database Connection Errors

```bash
# Test database connectivity
psql -h localhost -U crisisconnect -d crisisconnect

# Check if PostgreSQL is running
sudo systemctl status postgresql

# Verify pg_hba.conf allows connections
sudo tail /var/log/postgresql/postgresql-15-main.log
```

### High Memory Usage

```bash
# Check Java heap usage
docker exec crisisconnect-backend jstat -gc 1

# Adjust JVM settings in docker-compose.yml:
environment:
  JAVA_OPTS: "-Xmx2g -Xms1g"
```

### SSL Certificate Issues

```bash
# Renew Let's Encrypt certificate
sudo certbot renew --dry-run

# Check certificate expiry
sudo certbot certificates

# Force renewal
sudo certbot renew --force-renewal
```

---

## Production Checklist

Before going live, verify:

- [ ] All default passwords changed
- [ ] JWT_SECRET and ENCRYPTION_SECRET are cryptographically secure
- [ ] HTTPS/TLS enabled with valid certificate
- [ ] CORS configured for production domain only
- [ ] Admin bootstrap disabled after first admin creation
- [ ] Database backups configured and tested
- [ ] Firewall rules in place
- [ ] Nginx reverse proxy configured with security headers
- [ ] Rate limiting enabled
- [ ] Monitoring and alerting configured
- [ ] Log rotation configured
- [ ] Recovery plan documented
- [ ] Security audit performed
- [ ] Load testing completed
- [ ] Backup restoration tested

---

## Support

For deployment issues:
- GitHub Issues: https://github.com/yourusername/CrisisConnect/issues
- Documentation: See docs/ folder
- Security issues: sekacorn@gmail.com

---

**Last Updated:** 2026-01-23
**Version:** 1.0.0
