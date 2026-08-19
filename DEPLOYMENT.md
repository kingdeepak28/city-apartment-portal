# Production Deployment

Deploys the full stack (Postgres + Spring Boot backend + React frontend) behind nginx with a
Let's Encrypt certificate, using Docker Compose. Sized and documented for **city-apartments.in**
serving up to ~4,000 registered residents.

Written for a plain Ubuntu/Debian server. **On a GoDaddy VPS**, see
**[DEPLOYMENT-GODADDY.md](DEPLOYMENT-GODADDY.md)** first for GoDaddy's own dashboard steps
(getting VPS access, pointing DNS at it) - it hands off back to this doc for the actual server
setup, which needs nothing GoDaddy-specific changed once you're on Ubuntu 24.04.

## 1. Server sizing

4,000 *registered* users is not a high-traffic workload - real concurrency (people actively
clicking around at the same moment) will be a small fraction of that, even during a busy period
like a new notice going out. A single modest VPS handles this comfortably:

| Spec | Recommendation |
|---|---|
| vCPU | 2 (4 gives more headroom, not required) |
| RAM | 4 GB minimum, 8 GB comfortable |
| Disk | 40 GB+ SSD (grows with uploaded report/notice attachments - watch `df -h` over time) |
| OS | Ubuntu 22.04/24.04 LTS or Debian 12 |

This doesn't need a cluster, load balancer, or managed Kubernetes - that's over-engineering for
this scale and adds ongoing cost/complexity without a real benefit here. Revisit only if you
later see the server itself become the bottleneck (`docker stats`, sustained high CPU/memory).

## 2. One-time server setup

```bash
# Install Docker Engine + Compose plugin (Ubuntu/Debian):
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"     # log out/in once for this to take effect

# Firewall: only SSH, HTTP, HTTPS from the internet. Everything else (postgres, backend) is only
# reachable inside the Docker network, never exposed to the host's public interface.
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

**DNS**: point `city-apartments.in` and `www.city-apartments.in` A records at the server's public
IP before continuing - Let's Encrypt's HTTP-01 challenge needs them resolving correctly.

## 3. Get the code and configure

```bash
git clone <your-repo-url> society-document-portal   # or scp/rsync the tree over
cd society-document-portal
cp deploy/.env.prod.example .env
nano .env
```

Fill in every `CHANGE_ME` in `.env`:

- `DB_PASSWORD` - `openssl rand -base64 24`
- `JWT_SECRET` - `openssl rand -base64 48` (must be 32+ bytes; rotating it logs everyone out)
- `MAIL_USERNAME` / `MAIL_PASSWORD` / `MAIL_FROM` - real SMTP credentials (a Gmail App Password
  works for low volume; a transactional provider like SES/Postmark/SendGrid is worth it once
  you're sending real volume to 4,000 residents - Gmail's sending limits and deliverability
  reputation aren't built for that)
- `LETSENCRYPT_EMAIL` - gets certificate-expiry warnings, use a real inbox you check

`.env` contains real secrets - it's already gitignored. Don't commit it, and keep a copy
somewhere safe outside the server (a password manager or secrets vault, not a chat log).

## 4. First launch

```bash
./deploy/init-letsencrypt.sh          # one-time: builds images, issues the real TLS cert
```

This builds everything, boots Postgres + the backend, bootstraps a throwaway certificate so
nginx can start, obtains the real Let's Encrypt certificate, then reloads nginx with it and
starts the certbot auto-renewal loop. Takes a few minutes (Maven/npm builds included).

Once it finishes, `https://city-apartments.in` should load the login page with a trusted
certificate. Verify:

```bash
docker compose -f docker-compose.prod.yml ps        # everything "healthy" / "Up"
curl -I https://city-apartments.in
```

## 5. Immediately after first launch

Flyway seeds two accounts on a fresh database so the app is usable out of the box - both need
attention before real residents show up:

1. **Log in as Super Admin** (`super.admin@societyportal.local` / `Admin@123`) and change that
   password immediately (Profile → Change Password). Anyone who reads this repo's README knows
   the default.
2. **Delete or deactivate the seeded "Demo Member"** account (`demo.member@societyportal.local`)
   via Admin → Users - it's fake test data, not a real resident, and shares the same
   publicly-known default password.
3. Set your real **Society Settings** (Admin → Settings) - name, contact info, etc.
4. Do one real end-to-end pass: register a test resident, approve it as admin, publish a notice,
   confirm the notice/OTP emails actually arrive (SMS will just log to `docker compose logs
   backend` until a real SMS provider is wired into `SmsService.java` - see the main README).

## 6. Keeping it running

**Cron** (`crontab -e`), both scripts are safe to run daily regardless of whether anything is
actually due:

```cron
# Reload nginx daily so a renewed Let's Encrypt cert actually gets picked up
15 3 * * * /path/to/society-document-portal/deploy/renew-and-reload.sh >> /var/log/sdp-renew.log 2>&1

# Nightly DB + uploads backup, keeps 14 days locally - copy these off-box too (rsync/S3/etc.)
30 2 * * * /path/to/society-document-portal/deploy/backup.sh >> /var/log/sdp-backup.log 2>&1
```

**Logs**:
```bash
docker compose -f docker-compose.prod.yml logs -f backend    # app logs
docker compose -f docker-compose.prod.yml logs -f frontend   # nginx access/error logs
```

**Restart / redeploy after a code change**:
```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```
This rebuilds only what changed and does a rolling restart of that service; Postgres data and
uploaded files live in named Docker volumes and are untouched by rebuilds/restarts.

**Restore from backup** (disaster recovery):
```bash
gunzip -c backups/db_<timestamp>.sql.gz | docker compose -f docker-compose.prod.yml exec -T postgres psql -U society_admin -d society_portal
docker compose -f docker-compose.prod.yml exec -T backend tar xzf - -C /data < backups/uploads_<timestamp>.tar.gz
```

## 7. What's already handled for you

- **TLS**: automated Let's Encrypt issuance + renewal (`deploy/init-letsencrypt.sh`, the
  `certbot` service).
- **Reverse proxy**: nginx serves the built SPA, proxies `/api/*` to the backend, gzips
  responses, long-caches hashed static assets, and enforces HSTS.
- **Least exposure**: only nginx (80/443) is reachable from outside the server; Postgres and the
  backend are internal-network-only.
- **No secrets baked into images**: everything sensitive comes from `.env` at container startup.
- **Sanitized error responses**: unexpected server errors return a generic message + a
  correlation id to the client and the full exception only to the server log - nothing internal
  leaks to a resident's browser (see `GlobalExceptionHandler.java`).
- **Right-sized connection pool**: `application-prod.yml` tunes Hikari for this scale without
  starving Postgres's own connection limit.
- **Resource limits** on every container (`mem_limit` in `docker-compose.prod.yml`) so one
  misbehaving service can't take down the whole box.

## 8. Worth doing next (not blocking launch)

- **Real SMS provider**: `SmsService.java` currently only logs; wire in a DLT-registered Indian
  SMS aggregator once you have one, per the code comment there.
- **Rate limiting** on `/api/auth/login` and the registration endpoints (e.g. via nginx
  `limit_req`) - there's currently account lockout after repeated failed logins
  (`app.security.max-failed-login-attempts`), but no request-rate throttling in front of it.
- **Off-box backups**: the cron job above keeps 14 days *on* the server - also sync `backups/` to
  S3/Backblaze/etc. so a lost/compromised server doesn't take your only backup copy with it.
- **Centralized log shipping / uptime monitoring** if you want alerts before a resident tells you
  something's down.
