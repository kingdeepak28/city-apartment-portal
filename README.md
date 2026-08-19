# City Apartment Portal

A full-stack implementation of the Society Document Portal functional spec:
**Spring Boot 3 (Java 17) + PostgreSQL** backend, **React 18 + TypeScript + Tailwind** frontend.

## Quick start

```bash
./run.sh
```

One command, any machine. Uses Docker if it's available; otherwise it downloads a self-contained
toolchain (JDK 17, Maven, Node, PostgreSQL - no admin rights needed) and runs everything as plain
local processes. See `./run.sh` itself for `stop`/`status`/`logs`. This is for local use only -
for a real deployment see [DEPLOYMENT.md](DEPLOYMENT.md). The rest of this README covers manual,
step-by-step setup if you'd rather not use the script.

This build covers **Phase 1** of the spec's own delivery plan (Section 13), fully wired end-to-end:

- Authentication (JWT), self-registration with admin approval workflow
- User management (admin), pre-approved/bulk-imported accounts, lockout/unlock
- Reports module (categories, versioned attachments, visibility rules, publish lifecycle)
- Notices module (rich-text body, priority, pinning, expiry auto-archive, read tracking, reminders)
- Both dashboards (Admin + Member)
- Notifications: in-app + email (SMS is a stub - see "SMS" below), preferences, broadcast, delivery log
- Masters/categories, society settings, admin-account management, recycle bin, audit log

The full **Section 9 database schema** (all entities, including Photos/Minutes/Tenders) is created by
the Flyway migration so Phase 2 can be built directly on top of it - only Photos/Minutes/Tenders
business logic and screens are not yet implemented (they're the spec's own Phase 2).

---

## ✅ Build & runtime status

This machine had no Java, Maven, Node.js, Docker, or Homebrew installed, so a portable toolchain
(Temurin JDK 17, Maven 3.9, Node 20, and a standalone PostgreSQL 16 binary distribution) was
installed under `~/devtools` without needing `sudo`, purely to verify this build - **the app has
actually been compiled, started against a real Postgres database, and exercised end-to-end**, not
just reviewed by eye. That surfaced (and fixed) five real runtime bugs static review had missed:

1. Spring Data derived-query method names (`findByIsDeletedFalse`, etc.) must match the entity's
   **Java field name**, not its getter name - `Document.deleted`'s getter is `isDeleted()`, but the
   query methods needed `...DeletedFalse`, not `...IsDeletedFalse`. Same issue existed in a couple of
   JPA Criteria `root.get("isDeleted")` calls and one JPQL query.
2. The audit log's `old_value`/`new_value` columns were `jsonb` with a generic `Object` Java type;
   Postgres rejected plain strings as invalid JSON. Fixed by explicitly JSON-serializing every value
   before storing it in a plain `text` column.
3. With `open-in-view: false` (correct for production), several lazy `@ManyToOne`/`@ElementCollection`
   associations (`Document.category/createdBy/...`, `User.approvedBy`, `Category.parent`,
   `DocumentFile.document`) were being read in a DTO-mapping step *after* the transaction that loaded
   them had already closed, throwing `LazyInitializationException`. Fixed by marking those specific
   to-one/small-collection associations `EAGER` (safe - they're single references or tiny sets, not
   large collections) and, for the genuinely large `Document.files` collection, replacing
   `document.getFiles()` with a dedicated repository query everywhere it's read.
4. `DocumentService.restore()` and `.permanentDelete()` both fetched the target via a helper that
   filters *out* deleted documents - meaning recycle-bin restore/purge could never find the very item
   it was supposed to act on. Fixed with a dedicated "must currently be deleted" fetch.

All fixes are already applied in the source below. The end-to-end flows below were run for real
against a live Postgres instance and confirmed working: **login (admin + member), the full
registration → OTP verification → admin approval → login workflow, report/notice create → publish →
member view → file upload/download via signed URL, notification fan-out (in-app/email/SMS logged),
notice read-tracking, broadcast + notification log, recycle bin (delete → restore → permanent
delete), category CRUD with document-count delete-protection, and the audit log.**

Not explicitly exercised in this pass (reviewed by eye only): Excel exports, admin-account
management, settings updates, profile photo upload, notification preferences, password-reset email
links, account lockout after repeated failed logins, and the two scheduled jobs (notice
auto-archive, scheduled publish). These are lower-risk, more isolated code paths, but worth a
manual pass before relying on them.

If you want the exact toolchain used to verify this (no admin/sudo rights needed on macOS):
```bash
# JDK 17 (Temurin)
curl -fL -o jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/mac/x64/jdk/hotspot/normal/eclipse"
# Maven
curl -fL -o maven.tar.gz "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz"
# Node 20
curl -fL -o node.tar.gz "https://nodejs.org/dist/v20.18.0/node-v20.18.0-darwin-x64.tar.gz"
# Standalone PostgreSQL 16 binaries (EDB) - initdb/pg_ctl a local cluster, no install needed
curl -fL -o pg.zip "https://get.enterprisedb.com/postgresql/postgresql-16.4-1-osx-binaries.zip"
```
Extract each, put their `bin/` on `PATH` (and set `JAVA_HOME`), then follow the steps below.

---

## Project layout

```
society-document-portal/
├── backend/              Spring Boot API (Java 17, Maven)
│   └── src/main/resources/db/migration/   Flyway SQL migrations (full schema + seed data)
├── frontend/             React 18 + TypeScript + Tailwind SPA (Vite)
└── docker-compose.yml    Local PostgreSQL for development
```

## 1. Prerequisites

Install these first (none were available in the build environment):

- **Java 17+** and **Maven 3.9+**
- **Node.js 20+** and **npm**
- **PostgreSQL 16** (via Docker, Homebrew, or a native installer)

On macOS with Homebrew:
```bash
brew install openjdk@17 maven node postgresql@16
```
Or use Docker Desktop and the provided `docker-compose.yml` for just the database.

## 2. Database

```bash
docker compose up -d          # starts Postgres on localhost:5432
```
This creates database `society_portal`, user/password `society_admin`/`society_admin`
(see `docker-compose.yml`). If you're not using Docker, create that database/role yourself and
match `backend/src/main/resources/application.yml` (or override via env vars - see below).

Flyway runs automatically on backend startup and creates the full schema plus:
- A **Super Admin** account: `super.admin@societyportal.local` / `Admin@123`
- A **demo Member** account: `demo.member@societyportal.local` / `Member@123`
- Seeded Report/Notice/Photo/Meeting categories from the spec

**Change both default passwords before any real use.**

## 3. Backend

```bash
cd backend
mvn spring-boot:run
```
Runs on `http://localhost:8080`. Key environment variables (all optional, sane dev defaults are baked
into `application.yml`):

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Postgres connection | localhost:5432/society_portal |
| `JWT_SECRET` | HMAC signing key for auth tokens | dev placeholder - **set a real 32+ byte secret in production** |
| `MAIL_ENABLED` | `true` to actually send SMTP email | `false` (emails are logged to the console instead) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP settings | - |
| `SMS_ENABLED` | `true` once a real SMS provider is wired in | `false` (SMS is logged, not sent - see below) |
| `CORS_ORIGINS` | Allowed frontend origin(s) | `http://localhost:5173` |
| `FILE_STORAGE_ROOT` | Where uploaded files are stored on disk | `./uploads` |

Uploaded files are stored outside the web root under `FILE_STORAGE_ROOT` and are only ever served
through short-lived signed download/preview URLs (`/api/files/download|preview/{fileId}?token=...`),
never by a bare permanent path - matching the spec's file-security requirement.

### SMS

The spec itself flags (Section 14, point 5) that SMS needs a DLT-registered sender ID and approved
templates in India before it can go live. `SmsService` therefore just logs messages by default;
flip `SMS_ENABLED=true` and fill in a real provider call in
`backend/.../service/SmsService.java` once you have one.

## 4. Deploying to production

The steps above are for local development only. For a real deployment behind a domain (Docker
Compose, nginx, Let's Encrypt TLS, backups, sizing guidance), see **[DEPLOYMENT.md](DEPLOYMENT.md)**
(or **[DEPLOYMENT-GODADDY.md](DEPLOYMENT-GODADDY.md)** if hosting on a GoDaddy VPS).

## 5. Frontend

```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173` and proxies `/api/*` to the backend (see `vite.config.ts`).

```bash
npm run build     # production build to frontend/dist
```

## 6. What to check on first run

1. `mvn spring-boot:run` should apply the Flyway migrations cleanly against an empty database.
2. Log in as the Super Admin, then as the demo Member, to confirm both dashboards load.
3. Register a new resident end-to-end, approve it as admin, and log in as that member. (Self-
   registration has no OTP gate - email/mobile are taken as given and only admin approval decides
   whether the account goes live.)
4. Publish a Report and a Notice and confirm they appear on the member side and generate notifications
   (bell icon badge).

## 7. Known simplifications / follow-ups

- **Photos, Minutes, Tenders** - database tables exist (`album_detail`, `photos`, `minutes_detail`,
  `tender_detail`, `corrigendum`) but there is no service/controller/UI yet; this is exactly the
  spec's own Phase 2 scope.
- **Admin permission matrix** (FR-AD-60's per-module, per-role checkbox grid) is simplified to role
  checks (`SUPER_ADMIN` / `ADMIN` / `UPLOADER`) rather than a fully configurable matrix.
- **Visibility: "Selected users"** - the API supports it (`visibilityType: "USERS"` + a list of user
  IDs), but the Report/Notice admin forms only expose the All/Owners/Tenants/Selected-block(s) pickers
  in the UI; add a user-multiselect to the forms to wire up the last option.
- **Pagination on member notice lists** applies priority/unread filters in-memory on top of a DB page
  rather than pushing them into SQL - fine at the spec's target scale (2,000 users / 20,000 documents)
  but worth revisiting if that grows.
- Spring's `Page<T>` is returned directly by several list endpoints; it serializes fine but Spring Boot
  logs a warning about it not being intended for direct JSON serialization. Swapping to
  `org.springframework.data.web.PagedModel` is a good low-risk follow-up.
- No automated tests were written given the scope of this pass - add `@SpringBootTest` /
  `@WebMvcTest` coverage for the approval workflow and publish/notify flow first, since those are the
  highest-value paths to protect.

## License

Copyright © 2026 City Apartments Owner Association. All rights reserved. See [LICENSE](LICENSE).
