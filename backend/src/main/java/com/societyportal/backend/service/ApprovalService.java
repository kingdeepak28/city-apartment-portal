package com.societyportal.backend.service;

import com.societyportal.backend.domain.AdminUser;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.domain.enums.ResidentType;
import com.societyportal.backend.domain.enums.UserStatus;
import com.societyportal.backend.dto.UserDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.AdminUserRepository;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.approval-sla-days}")
    private int slaDays;

    public Page<UserDtos.UserSummary> pendingQueue(String block, String residentType, LocalDate from, LocalDate to,
                                                     String keyword, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), UserStatus.PENDING));
            if (block != null && !block.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("block")), block.toLowerCase()));
            }
            if (residentType != null && !residentType.isBlank()) {
                predicates.add(cb.equal(root.get("residentType"), ResidentType.valueOf(residentType.toUpperCase())));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registeredOn"), from.atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("registeredOn"), to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("flatNo")), like),
                        cb.like(root.get("mobile"), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        OffsetDateTime slaCutoff = OffsetDateTime.now().minusDays(slaDays);
        return userRepository.findAll(spec, pageable)
                .map(u -> UserDtos.UserSummary.from(u, "/api/files/registration-proof/" + u.getId(),
                        u.getRegisteredOn().isBefore(slaCutoff)));
    }

    public List<UserDtos.UserSummary> recentPending(int limit) {
        return userRepository.findTop5ByStatusOrderByRegisteredOnDesc(UserStatus.PENDING).stream()
                .limit(limit)
                .map(u -> UserDtos.UserSummary.from(u, "/api/files/registration-proof/" + u.getId(), false))
                .toList();
    }

    public long overdueCount() {
        return userRepository.countByStatusAndRegisteredOnBefore(UserStatus.PENDING,
                OffsetDateTime.now().minusDays(slaDays));
    }

    @Transactional
    public void approve(UUID userId) {
        User user = getPending(userId);
        AdminUser approver = getApprover();
        user.setStatus(UserStatus.ACTIVE);
        user.setApprovedBy(approver);
        user.setApprovedOn(OffsetDateTime.now());
        user.setRejectionReason(null);
        user.setRejectionRemarks(null);
        userRepository.save(user);

        auditService.log("APPROVAL", "APPROVE", user.getId().toString(), UserStatus.PENDING, UserStatus.ACTIVE);
        notificationService.notifyMembers(List.of(user), NotificationType.REGISTRATION_APPROVED,
                "Your registration has been approved",
                "Welcome to the Society Document Portal! You can now log in using your registered email/mobile.",
                "/login", true, null);
    }

    @Transactional
    public void reject(UUID userId, String reason, String remarks) {
        User user = getPending(userId);
        user.setStatus(UserStatus.REJECTED);
        user.setRejectionReason(reason);
        user.setRejectionRemarks(remarks);
        userRepository.save(user);

        auditService.log("APPROVAL", "REJECT", user.getId().toString(), UserStatus.PENDING, UserStatus.REJECTED);
        notificationService.notifyMembers(List.of(user), NotificationType.REGISTRATION_REJECTED,
                "Your registration was not approved",
                "Reason: " + reason + (remarks != null && !remarks.isBlank() ? " - " + remarks : "")
                        + ". You may correct the details and re-apply.",
                "/register", true, null);
    }

    @Transactional
    public void requestInfo(UUID userId, String note) {
        User user = getPending(userId);
        user.setStatus(UserStatus.INFO_REQUESTED);
        user.setInfoRequestedNote(note);
        userRepository.save(user);

        auditService.log("APPROVAL", "REQUEST_INFO", user.getId().toString(), UserStatus.PENDING, UserStatus.INFO_REQUESTED);
        notificationService.notifyMembers(List.of(user), NotificationType.INFO_REQUESTED,
                "More information needed for your registration",
                note, "/register/edit/" + user.getId(), true, null);
    }

    @Transactional
    public void bulkApprove(List<UUID> userIds) {
        userIds.forEach(this::approve);
    }

    @Transactional
    public void bulkReject(List<UUID> userIds, String reason, String remarks) {
        userIds.forEach(id -> reject(id, reason, remarks));
    }

    private User getPending(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getStatus() != UserStatus.PENDING && user.getStatus() != UserStatus.INFO_REQUESTED) {
            throw ApiException.badRequest("This request has already been processed");
        }
        return user;
    }

    private AdminUser getApprover() {
        return adminUserRepository.findById(CurrentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Approver account not found"));
    }
}
