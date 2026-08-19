// Author: deepak.maheshwari

package com.societyportal.backend.domain;

import com.societyportal.backend.domain.enums.DeliveryStatus;
import com.societyportal.backend.domain.enums.NotificationChannel;
import com.societyportal.backend.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A single notification on a single channel, targeted at either a member or an admin. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    /** Groups every recipient/channel row created from the same publish/broadcast event. */
    @Column(name = "batch_id", nullable = false)
    @Builder.Default
    private UUID batchId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private AdminUser adminUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(length = 500)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 10)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
