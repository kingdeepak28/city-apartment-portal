package com.societyportal.backend.service;

import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.domain.enums.ResidentType;
import com.societyportal.backend.domain.enums.UserStatus;
import com.societyportal.backend.dto.UserDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private static final SecureRandom RANDOM = new SecureRandom();

    public Page<UserDtos.UserSummary> list(String status, String block, String residentType,
                                            LocalDate from, LocalDate to, String keyword, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), UserStatus.valueOf(status.toUpperCase())));
            }
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
        return userRepository.findAll(spec, pageable)
                .map(u -> UserDtos.UserSummary.from(u, "/api/files/registration-proof/" + u.getId(), false));
    }

    public User getOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional
    public User createPreApproved(UserDtos.CreateUserRequest req) {
        String password = (req.getPassword() == null || req.getPassword().isBlank())
                ? generateTempPassword() : req.getPassword();
        User user = User.builder()
                .name(req.getFullName())
                .email(req.getEmail())
                .mobile(req.getMobile())
                .flatNo(req.getFlatNo())
                .block(req.getBlock())
                .residentType(ResidentType.valueOf(req.getResidentType().toUpperCase()))
                .status(UserStatus.ACTIVE)
                .passwordHash(passwordEncoder.encode(password))
                .emailVerified(true)
                .mobileVerified(true)
                .approvedOn(java.time.OffsetDateTime.now())
                .build();
        user = userRepository.save(user);
        auditService.log("USER", "ADMIN_CREATED", user.getId().toString(), null, null);
        notificationService.notifyMembers(List.of(user), NotificationType.REGISTRATION_APPROVED,
                "Your Society Document Portal account is ready",
                "An account has been created for you. Temporary password: " + password
                        + " - please log in and change it immediately.",
                "/login", true, null);
        return user;
    }

    @Transactional
    public UserDtos.BulkImportResult bulkImport(org.springframework.web.multipart.MultipartFile csv) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // name,flatNo,block,residentType,mobile,email
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 6) {
                    errors.add("Row " + rowNum + ": expected 6 columns (name,flatNo,block,residentType,mobile,email)");
                    continue;
                }
                try {
                    UserDtos.CreateUserRequest req = new UserDtos.CreateUserRequest();
                    req.setFullName(cols[0].trim());
                    req.setFlatNo(cols[1].trim());
                    req.setBlock(cols[2].trim());
                    req.setResidentType(cols[3].trim());
                    req.setMobile(cols[4].trim());
                    req.setEmail(cols[5].trim());
                    if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
                        errors.add("Row " + rowNum + ": email already exists (" + req.getEmail() + ")");
                        continue;
                    }
                    if (userRepository.existsByMobile(req.getMobile())) {
                        errors.add("Row " + rowNum + ": mobile already exists (" + req.getMobile() + ")");
                        continue;
                    }
                    createPreApproved(req);
                    success++;
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded CSV file");
        }
        return new UserDtos.BulkImportResult(success, errors);
    }

    @Transactional
    public void updateStatus(UUID userId, String status) {
        User user = getOrThrow(userId);
        UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
        UserStatus old = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);
        auditService.log("USER", "STATUS_CHANGE", userId.toString(), old, newStatus);
        if (newStatus == UserStatus.SUSPENDED) {
            notificationService.notifyMembers(List.of(user), NotificationType.ACCOUNT_SUSPENDED,
                    "Your account has been suspended",
                    "Your access to the Society Document Portal has been suspended by the administrator.",
                    "/login", true, null);
        }
    }

    @Transactional
    public void delete(UUID userId) {
        User user = getOrThrow(userId);
        userRepository.delete(user);
        auditService.log("USER", "DELETE", userId.toString(), user.getStatus(), null);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
