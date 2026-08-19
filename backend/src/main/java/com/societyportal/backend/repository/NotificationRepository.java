// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.Notification;
import com.societyportal.backend.domain.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, NotificationChannel channel, Pageable pageable);

    Page<Notification> findByUserIdAndChannelAndReadOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, boolean read, Pageable pageable);

    long countByUserIdAndChannelAndReadFalse(UUID userId, NotificationChannel channel);

    Page<Notification> findByAdminUserIdAndChannelOrderByCreatedAtDesc(UUID adminUserId, NotificationChannel channel, Pageable pageable);

    long countByAdminUserIdAndChannelAndReadFalse(UUID adminUserId, NotificationChannel channel);

    List<Notification> findByBatchId(UUID batchId);

    @org.springframework.data.jpa.repository.Query(
            "select distinct n.batchId from Notification n order by n.batchId desc")
    Page<UUID> findDistinctBatchIds(Pageable pageable);
}
