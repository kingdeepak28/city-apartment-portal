-- Society Document Portal - initial schema
-- Covers all entities from the functional spec (Section 9).
-- Phase 1 modules (Auth, Registration/Approval, Reports, Notices, Notifications,
-- Dashboards, User management, Audit log) are fully backed by services/APIs.
-- Phase 2 tables (albums/photos, minutes, tenders/corrigenda) are schema-ready
-- scaffolding for a future phase and are not yet wired to business logic.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ======================================================================
-- Admin-side accounts (Super Admin / Admin / Content Uploader)
-- ======================================================================
CREATE TABLE admin_users (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(150)  NOT NULL,
    email                 VARCHAR(150)  NOT NULL UNIQUE,
    mobile                VARCHAR(20),
    password_hash         VARCHAR(200)  NOT NULL,
    role                  VARCHAR(20)   NOT NULL CHECK (role IN ('SUPER_ADMIN','ADMIN','UPLOADER')),
    permissions           JSONB         NOT NULL DEFAULT '{}',
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
    failed_login_attempts INTEGER       NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    last_login            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ======================================================================
-- Members / residents
-- ======================================================================
CREATE TABLE users (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(150) NOT NULL,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    mobile                  VARCHAR(20)  NOT NULL UNIQUE,
    password_hash           VARCHAR(200) NOT NULL,
    flat_no                 VARCHAR(30)  NOT NULL,
    block                   VARCHAR(30)  NOT NULL,
    resident_type           VARCHAR(10)  NOT NULL CHECK (resident_type IN ('OWNER','TENANT')),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING','ACTIVE','REJECTED','SUSPENDED','INFO_REQUESTED')),
    proof_file_path         VARCHAR(500),
    photo_path              VARCHAR(500),
    email_verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    mobile_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    registered_on           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    approved_by             UUID REFERENCES admin_users(id),
    approved_on             TIMESTAMPTZ,
    rejection_reason        VARCHAR(100),
    rejection_remarks       VARCHAR(500),
    info_requested_note     VARCHAR(500),
    failed_login_attempts   INTEGER      NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_login              TIMESTAMPTZ,
    notification_preferences JSONB       NOT NULL DEFAULT '{"email": true, "sms": true}',
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_block ON users(block);

-- ======================================================================
-- OTP tokens (registration email/mobile verification, login OTP, reset)
-- ======================================================================
CREATE TABLE otp_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact     VARCHAR(150) NOT NULL,
    purpose     VARCHAR(30)  NOT NULL CHECK (purpose IN
                    ('REGISTER_EMAIL','REGISTER_MOBILE','LOGIN','RESET_PASSWORD')),
    otp_code    VARCHAR(10)  NOT NULL,
    verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_contact_purpose ON otp_tokens(contact, purpose);

-- ======================================================================
-- Password reset tokens
-- ======================================================================
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_type VARCHAR(10) NOT NULL CHECK (account_type IN ('USER','ADMIN')),
    account_id  UUID NOT NULL,
    token       VARCHAR(200) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ======================================================================
-- Master categories - shared across content types (report/notice/photo/
-- meeting/tender), one level of nesting via parent_id
-- ======================================================================
CREATE TABLE categories (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR(20)  NOT NULL CHECK (type IN ('REPORT','NOTICE','PHOTO','MEETING','TENDER')),
    name           VARCHAR(150) NOT NULL,
    parent_id      UUID REFERENCES categories(id),
    display_order  INTEGER      NOT NULL DEFAULT 0,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (type, name, parent_id)
);

-- ======================================================================
-- Documents - generic container for Reports, Notices, and (phase 2)
-- Photo albums / Minutes / Tenders. Type-specific fields live in
-- `metadata` (jsonb) plus the phase-2 detail tables below.
-- ======================================================================
CREATE TABLE documents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type     VARCHAR(20)  NOT NULL CHECK (content_type IN ('REPORT','NOTICE','PHOTO','MINUTES','TENDER')),
    title            VARCHAR(300) NOT NULL,
    category_id      UUID REFERENCES categories(id),
    sub_category_id  UUID REFERENCES categories(id),
    description      TEXT,
    tags             VARCHAR(500),
    metadata         JSONB        NOT NULL DEFAULT '{}',
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    visibility_type  VARCHAR(20)  NOT NULL DEFAULT 'ALL'
                          CHECK (visibility_type IN ('ALL','OWNERS','TENANTS','BLOCKS','USERS')),
    publish_at       TIMESTAMPTZ,
    published_on     TIMESTAMPTZ,
    expiry_at        TIMESTAMPTZ,
    is_pinned        BOOLEAN      NOT NULL DEFAULT FALSE,
    view_count       INTEGER      NOT NULL DEFAULT 0,
    download_count   INTEGER      NOT NULL DEFAULT 0,
    created_by       UUID REFERENCES admin_users(id),
    updated_by       UUID REFERENCES admin_users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_documents_content_type ON documents(content_type);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_category ON documents(category_id);
CREATE INDEX idx_documents_deleted ON documents(is_deleted);

CREATE TABLE document_visibility_blocks (
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    block        VARCHAR(30) NOT NULL,
    PRIMARY KEY (document_id, block)
);

CREATE TABLE document_visibility_users (
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, user_id)
);

-- ======================================================================
-- Files / attachments with version history
-- ======================================================================
CREATE TABLE document_files (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    file_name     VARCHAR(300) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    mime_type     VARCHAR(150),
    file_size     BIGINT,
    version_no    INTEGER NOT NULL DEFAULT 1,
    is_current    BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_by   UUID REFERENCES admin_users(id),
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_files_document ON document_files(document_id);

-- ======================================================================
-- Notice read tracking
-- ======================================================================
CREATE TABLE notice_read_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, user_id)
);

-- ======================================================================
-- Notifications (in-app / email / sms) - can target a member or an admin
-- ======================================================================
CREATE TABLE notifications (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id         UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID REFERENCES users(id) ON DELETE CASCADE,
    admin_user_id    UUID REFERENCES admin_users(id) ON DELETE CASCADE,
    type             VARCHAR(30) NOT NULL,
    title            VARCHAR(300) NOT NULL,
    body             VARCHAR(1000),
    link             VARCHAR(500),
    channel          VARCHAR(10) NOT NULL CHECK (channel IN ('IN_APP','EMAIL','SMS')),
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_status  VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (delivery_status IN ('PENDING','SENT','FAILED')),
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (user_id IS NOT NULL OR admin_user_id IS NOT NULL)
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_admin ON notifications(admin_user_id, is_read);
CREATE INDEX idx_notifications_batch ON notifications(batch_id);

-- ======================================================================
-- Document access log (views/downloads) - powers engagement analytics
-- ======================================================================
CREATE TABLE document_access_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id      UUID REFERENCES users(id),
    action       VARCHAR(10) NOT NULL CHECK (action IN ('VIEW','DOWNLOAD')),
    ip           VARCHAR(50),
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_access_log_document ON document_access_log(document_id);

-- ======================================================================
-- Audit log - every mutating action across the system
-- ======================================================================
CREATE TABLE audit_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID,
    actor_type   VARCHAR(10) NOT NULL CHECK (actor_type IN ('ADMIN','MEMBER','SYSTEM')),
    actor_name   VARCHAR(150),
    module       VARCHAR(50) NOT NULL,
    action       VARCHAR(50) NOT NULL,
    record_id    VARCHAR(100),
    old_value    TEXT,
    new_value    TEXT,
    ip           VARCHAR(50),
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_module ON audit_log(module);
CREATE INDEX idx_audit_log_timestamp ON audit_log(occurred_at);

-- ======================================================================
-- Key/value settings (society profile, numbering formats, file limits...)
-- ======================================================================
CREATE TABLE settings (
    key          VARCHAR(100) PRIMARY KEY,
    value        TEXT,
    description  VARCHAR(300),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ======================================================================
-- Phase 2 scaffolding (not yet exposed via API/UI in this build)
-- ======================================================================
CREATE TABLE album_detail (
    document_id       UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    event_date        DATE,
    cover_photo_file_id UUID REFERENCES document_files(id),
    watermark_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    download_enabled  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE photos (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    file_id       UUID NOT NULL REFERENCES document_files(id) ON DELETE CASCADE,
    caption       VARCHAR(300),
    alt_text      VARCHAR(300),
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE minutes_detail (
    document_id       UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    meeting_type      VARCHAR(30),
    meeting_datetime  TIMESTAMPTZ,
    venue             VARCHAR(300),
    chairperson       VARCHAR(150),
    attendee_count    INTEGER,
    resolutions       TEXT,
    next_meeting_date DATE,
    is_provisional    BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tender_detail (
    document_id          UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    tender_no            VARCHAR(50) UNIQUE,
    estimated_value      NUMERIC(14,2),
    emd_amount           NUMERIC(14,2),
    document_fee         NUMERIC(14,2),
    issue_date           DATE,
    pre_bid_meeting_date TIMESTAMPTZ,
    submission_deadline  TIMESTAMPTZ,
    submission_mode      VARCHAR(300),
    contact_person       VARCHAR(150),
    contact_phone        VARCHAR(20),
    tender_status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                              CHECK (tender_status IN ('DRAFT','OPEN','CLOSED','UNDER_EVALUATION','AWARDED','CANCELLED')),
    awarded_to           VARCHAR(200),
    award_date           DATE,
    awarded_value        NUMERIC(14,2)
);

CREATE TABLE corrigendum (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    description  TEXT NOT NULL,
    file_id      UUID REFERENCES document_files(id),
    issued_on    TIMESTAMPTZ NOT NULL DEFAULT now()
);
