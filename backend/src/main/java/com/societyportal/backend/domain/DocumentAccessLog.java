// Author: deepak.maheshwari

package com.societyportal.backend.domain;

import com.societyportal.backend.domain.enums.AccessAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_access_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessLog {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccessAction action;

    @Column(length = 50)
    private String ip;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private OffsetDateTime occurredAt = OffsetDateTime.now();
}
