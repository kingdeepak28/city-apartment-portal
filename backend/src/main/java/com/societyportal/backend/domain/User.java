package com.societyportal.backend.domain;

import com.societyportal.backend.domain.enums.ResidentType;
import com.societyportal.backend.domain.enums.UserStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A resident / member account (Owner or Tenant). */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String mobile;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "flat_no", nullable = false, length = 30)
    private String flatNo;

    @Column(nullable = false, length = 30)
    private String block;

    @Enumerated(EnumType.STRING)
    @Column(name = "resident_type", nullable = false, length = 10)
    private ResidentType residentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "proof_file_path", length = 500)
    private String proofFilePath;

    @Column(name = "photo_path", length = 500)
    private String photoPath;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "mobile_verified", nullable = false)
    @Builder.Default
    private boolean mobileVerified = false;

    @Column(name = "registered_on", nullable = false)
    @Builder.Default
    private OffsetDateTime registeredOn = OffsetDateTime.now();

    // EAGER for the same reason as Document's to-one refs (see Document.java) - UserSummary always
    // needs approvedByName, and open-in-view is disabled so a LAZY proxy here would blow up as soon
    // as it's touched outside the transaction that loaded the User.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by")
    private AdminUser approvedBy;

    @Column(name = "approved_on")
    private OffsetDateTime approvedOn;

    @Column(name = "rejection_reason", length = 100)
    private String rejectionReason;

    @Column(name = "rejection_remarks", length = 500)
    private String rejectionRemarks;

    @Column(name = "info_requested_note", length = 500)
    private String infoRequestedNote;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Type(JsonType.class)
    @Column(name = "notification_preferences", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> notificationPreferences = new HashMap<>(Map.of("email", true, "sms", true));

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
