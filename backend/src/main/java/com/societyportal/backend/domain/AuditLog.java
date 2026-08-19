// Author: deepak.maheshwari

package com.societyportal.backend.domain;

import com.societyportal.backend.domain.enums.ActorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Immutable record of every mutating action, for compliance and traceability. */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private ActorType actorType;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(nullable = false, length = 50)
    private String module;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "record_id", length = 100)
    private String recordId;

    /** JSON-serialized snapshot of the value before the change (plain text, not jsonb - the values
     *  logged here are a grab-bag of strings/enums/maps, and jsonb would reject a bare string). */
    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(length = 50)
    private String ip;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private OffsetDateTime occurredAt = OffsetDateTime.now();
}
