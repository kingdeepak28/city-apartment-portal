# Deploying to a GoDaddy VPS

GoDaddy-specific walkthrough for `city-apartments.in` on a GoDaddy VPS running **Ubuntu 24.04
LTS**. Same underlying stack as [DEPLOYMENT.md](DEPLOYMENT.md) (Docker Compose, Let's Encrypt) -
this doc covers only the parts that are specific to GoDaddy's own dashboards (getting VPS access,
DNS), then hands off to DEPLOYMENT.md for the server setup and everything after, since those
commands already target Ubuntu/Debian and need nothing GoDaddy-specific changed.

## 1. Get access to the VPS

1. godaddy.com → **My Products** → find your VPS → **Manage**. Note the server's public IP.
2. GoDaddy emails the initial root password when the VPS is provisioned (or you set an SSH key
   during setup, if you chose that option). SSH in:
   ```bash
   ssh root@<your-vps-ip>
   ```
3. Confirm you actually got Ubuntu 24.04:
   ```bash
   lsb_release -a
   ```
   If this instead shows AlmaLinux/CentOS/Rocky (GoDaddy's default for cPanel-based VPS plans),
   you were given a different image than expected - either re-provision with Ubuntu 24.04
   selected explicitly, or use dnf/firewalld commands instead of apt/ufw for step 3 below.

## 2. Point city-apartments.in at this VPS (GoDaddy DNS)

1. godaddy.com → **My Products** → **Domains** → `city-apartments.in` → **DNS** (or "Manage DNS").
2. Under **Records**, add/edit:
   | Type | Name | Value | TTL |
   |---|---|---|---|
   | A | @ | `<your-vps-ip>` | 1 hour (or leave default) |
   | A | www | `<your-vps-ip>` | 1 hour |
3. If GoDaddy pre-populated a "Parked" A record or a Website Builder/forwarding record for `@`,
   remove or overwrite it - only one A record per name should point traffic anywhere.
4. DNS propagation is usually fast but can take up to a few hours. Check from your own machine:
   ```bash
   dig +short city-apartments.in
   dig +short www.city-apartments.in
   ```
   Both should eventually return `<your-vps-ip>`. **Wait for this before running
   `init-letsencrypt.sh`** in the next section - Let's Encrypt's domain-ownership check will fail
   if the domain doesn't resolve to this server yet.

## 3. Server prep and everything after: follow DEPLOYMENT.md directly

Ubuntu 24.04 needs nothing GoDaddy-specific here - continue at
**[DEPLOYMENT.md, section 2 ("One-time server setup")](DEPLOYMENT.md#2-one-time-server-setup)**
and follow it straight through: Docker install, `ufw`, cloning the repo, `.env`,
`init-letsencrypt.sh`, the post-launch checklist, and the cron jobs for cert-reload/backups.

Two things worth checking specifically because this is a GoDaddy VPS, not because it's Ubuntu:

- **GoDaddy's dashboard may have its own network firewall** in addition to the OS-level `ufw` you
  configure in DEPLOYMENT.md - check the VPS's **Networking**/**Firewall** tab in GoDaddy's panel
  and make sure 80/443/22 are allowed there too. `ufw` alone won't help if GoDaddy's own network
  layer is dropping the traffic before it reaches the server.
- **A pre-installed control panel**: if you picked a plan/image that bundled cPanel or Plesk
  despite choosing Ubuntu, something may already be bound to ports 80/443
  (`sudo ss -tlnp | grep -E ':80|:443'`). Our own nginx container needs those ports free - stop
  the other web server first, or this stack simply won't be able to bind them.

## GoDaddy-specific gotchas worth knowing about

- **GoDaddy's own DNS propagation** can occasionally lag longer than other registrars during peak
  periods - if `dig` isn't resolving after a few hours, GoDaddy's support chat can usually confirm
  the record actually saved correctly on their end.
- **Resource tier**: match VPS RAM to DEPLOYMENT.md's sizing guidance (4GB+ comfortable for
  ~4,000 users) - GoDaddy's entry VPS tiers (1-2GB) will struggle to run Postgres + the JVM
  backend + nginx together; upgrade the plan rather than fighting for memory if things feel slow.
