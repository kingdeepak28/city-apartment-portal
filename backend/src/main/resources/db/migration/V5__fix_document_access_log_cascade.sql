-- V1 gave every users(id) foreign key ON DELETE CASCADE except this one, which was left at the
-- default NO ACTION. In practice that means deleting (or promoting, which deletes the member row
-- as part of moving them to admin_users - see UserAdminService.promoteToAdmin) any user who has
-- ever viewed or downloaded a single report/notice fails outright with a foreign-key violation,
-- since document_access_log still points at them. Bring it in line with the other three
-- users(id) references (document_visibility_users, notice_read_log, notifications), which already
-- correctly cascade.
ALTER TABLE document_access_log DROP CONSTRAINT document_access_log_user_id_fkey;
ALTER TABLE document_access_log
    ADD CONSTRAINT document_access_log_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
