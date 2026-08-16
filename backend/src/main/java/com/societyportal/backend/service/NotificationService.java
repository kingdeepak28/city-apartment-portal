package com.societyportal.backend.service;

import com.societyportal.backend.domain.AdminUser;
import com.societyportal.backend.domain.Notification;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.DeliveryStatus;
import com.societyportal.backend.domain.enums.NotificationChannel;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.dto.NotificationDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.NotificationRepository;
import com.societyportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fans a single event out into one Notification row per recipient/channel.
 * In-app notifications are always created; email/SMS are created according
 * to each member's preferences unless {@code forceAllChannels} is set
 * (urgent notices, account-status events per the notification rules table).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    @Transactional
    public UUID notifyMembers(List<User> recipients, NotificationType type, String title, String body,
                               String link, boolean forceAllChannels, String prefCategory) {
        UUID batchId = UUID.randomUUID();
        for (User user : recipients) {
            saveAndDispatch(Notification.builder()
                    .batchId(batchId).user(user).type(type).title(title).body(body).link(link)
                    .channel(NotificationChannel.IN_APP).build(), null);

            if (forceAllChannels || prefEnabled(user, "email", prefCategory)) {
                Notification n = Notification.builder()
                        .batchId(batchId).user(user).type(type).title(title).body(body).link(link)
                        .channel(NotificationChannel.EMAIL).build();
                saveAndDispatch(n, () -> emailService.send(user.getEmail(), title, body));
            }
            if (forceAllChannels || prefEnabled(user, "sms", prefCategory)) {
                Notification n = Notification.builder()
                        .batchId(batchId).user(user).type(type).title(title).body(body).link(link)
                        .channel(NotificationChannel.SMS).build();
                saveAndDispatch(n, () -> smsService.send(user.getMobile(), title));
            }
        }
        return batchId;
    }

    @Transactional
    public void notifyAdmins(List<AdminUser> admins, NotificationType type, String title, String body, String link) {
        UUID batchId = UUID.randomUUID();
        for (AdminUser admin : admins) {
            saveAndDispatch(Notification.builder()
                    .batchId(batchId).adminUser(admin).type(type).title(title).body(body).link(link)
                    .channel(NotificationChannel.IN_APP).build(), null);
            Notification n = Notification.builder()
                    .batchId(batchId).adminUser(admin).type(type).title(title).body(body).link(link)
                    .channel(NotificationChannel.EMAIL).build();
            saveAndDispatch(n, () -> emailService.send(admin.getEmail(), title, body));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean prefEnabled(User user, String channelKey, String prefCategory) {
        if (prefCategory == null) {
            return true;
        }
        Map<String, Object> prefs = user.getNotificationPreferences();
        Object v = prefs.get(channelKey + "." + prefCategory);
        return !(v instanceof Boolean b) || b; // default true when unset
    }

    private void saveAndDispatch(Notification notification, java.util.function.BooleanSupplier dispatch) {
        if (notification.getChannel() == NotificationChannel.IN_APP) {
            notification.setDeliveryStatus(DeliveryStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
        } else {
            boolean ok = dispatch != null && dispatch.getAsBoolean();
            notification.setDeliveryStatus(ok ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
            notification.setSentAt(OffsetDateTime.now());
        }
        notificationRepository.save(notification);
    }

    public Page<NotificationDtos.NotificationItem> listForMember(UUID userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly != null && unreadOnly
                ? notificationRepository.findByUserIdAndChannelAndReadOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP, false, pageable)
                : notificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP, pageable);
        return page.map(this::toItem);
    }

    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndChannelAndReadFalse(userId, NotificationChannel.IN_APP);
    }

    public long unreadCountAdmin(UUID adminId) {
        return notificationRepository.countByAdminUserIdAndChannelAndReadFalse(adminId, NotificationChannel.IN_APP);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (n.getUser() == null || !n.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Not your notification");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        var page = notificationRepository.findByUserIdAndChannelAndReadOrderByCreatedAtDesc(
                userId, NotificationChannel.IN_APP, false, Pageable.unpaged());
        page.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(page.getContent());
    }

    @Transactional
    public void delete(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        if (n.getUser() == null || !n.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Not your notification");
        }
        notificationRepository.delete(n);
    }

    private NotificationDtos.NotificationItem toItem(Notification n) {
        return NotificationDtos.NotificationItem.builder()
                .id(n.getId()).type(n.getType().name()).title(n.getTitle()).body(n.getBody())
                .link(n.getLink()).read(n.isRead()).createdAt(n.getCreatedAt()).build();
    }

    @Transactional
    public UUID broadcast(NotificationDtos.BroadcastRequest req) {
        List<User> allActive = userRepository.findByStatus(com.societyportal.backend.domain.enums.UserStatus.ACTIVE);
        List<User> audience = switch (req.getAudienceType().toUpperCase()) {
            case "BLOCK" -> allActive.stream().filter(u -> req.getBlocks() != null && req.getBlocks().contains(u.getBlock())).toList();
            case "RESIDENT_TYPE" -> allActive.stream()
                    .filter(u -> req.getResidentType() != null && u.getResidentType().name().equalsIgnoreCase(req.getResidentType())).toList();
            case "USERS" -> allActive.stream().filter(u -> req.getUserIds() != null && req.getUserIds().contains(u.getId())).toList();
            default -> allActive;
        };
        UUID batchId = UUID.randomUUID();
        for (User user : audience) {
            saveAndDispatch(Notification.builder()
                    .batchId(batchId).user(user).type(NotificationType.BROADCAST).title(req.getTitle())
                    .body(req.getMessage()).channel(NotificationChannel.IN_APP).build(), null);
            if (req.isSendEmail()) {
                saveAndDispatch(Notification.builder()
                        .batchId(batchId).user(user).type(NotificationType.BROADCAST).title(req.getTitle())
                        .body(req.getMessage()).channel(NotificationChannel.EMAIL).build(),
                        () -> emailService.send(user.getEmail(), req.getTitle(), req.getMessage()));
            }
            if (req.isSendSms()) {
                saveAndDispatch(Notification.builder()
                        .batchId(batchId).user(user).type(NotificationType.BROADCAST).title(req.getTitle())
                        .body(req.getMessage()).channel(NotificationChannel.SMS).build(),
                        () -> smsService.send(user.getMobile(), req.getTitle()));
            }
        }
        return batchId;
    }

    public Page<NotificationDtos.NotificationLogItem> log(Pageable pageable) {
        return notificationRepository.findDistinctBatchIds(pageable).map(batchId -> {
            List<Notification> rows = notificationRepository.findByBatchId(batchId);
            Notification first = rows.get(0);
            long delivered = rows.stream().filter(n -> n.getDeliveryStatus() == DeliveryStatus.SENT).count();
            long read = rows.stream().filter(Notification::isRead).count();
            String channels = rows.stream().map(n -> n.getChannel().name()).distinct().sorted()
                    .reduce((a, b) -> a + ", " + b).orElse("");
            return NotificationDtos.NotificationLogItem.builder()
                    .id(batchId).title(first.getTitle())
                    .body("Recipients: " + rows.stream().map(n -> n.getUser() != null ? n.getUser().getId() : n.getAdminUser().getId())
                            .distinct().count() + " | Delivered: " + delivered + " | Read: " + read)
                    .channel(channels).deliveryStatus(first.getDeliveryStatus().name())
                    .sentAt(first.getSentAt()).recipientName(first.getType().name())
                    .build();
        });
    }

    public Map<String, Boolean> getPreferences(User user) {
        Map<String, Object> raw = user.getNotificationPreferences();
        Map<String, Boolean> prefs = new java.util.LinkedHashMap<>();
        for (String category : List.of("report", "notice", "minutes", "tender")) {
            prefs.put("email." + category, !(raw.get("email." + category) instanceof Boolean b) || b);
            prefs.put("sms." + category, !(raw.get("sms." + category) instanceof Boolean b2) || b2);
        }
        return prefs;
    }

    @Transactional
    public void updatePreferences(User user, Map<String, Boolean> updates) {
        user.getNotificationPreferences().putAll(updates);
        userRepository.save(user);
    }

    @Transactional
    public int resendFailed(UUID batchId) {
        List<Notification> failed = notificationRepository.findByBatchId(batchId).stream()
                .filter(n -> n.getDeliveryStatus() == DeliveryStatus.FAILED)
                .toList();
        for (Notification n : failed) {
            boolean ok = switch (n.getChannel()) {
                case EMAIL -> n.getUser() != null && emailService.send(n.getUser().getEmail(), n.getTitle(), n.getBody());
                case SMS -> n.getUser() != null && smsService.send(n.getUser().getMobile(), n.getTitle());
                case IN_APP -> true;
            };
            n.setDeliveryStatus(ok ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
            n.setSentAt(OffsetDateTime.now());
        }
        notificationRepository.saveAll(failed);
        return failed.size();
    }
}
