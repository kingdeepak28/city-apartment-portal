// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.domain.enums.ResidentType;
import com.societyportal.backend.domain.enums.UserStatus;
import com.societyportal.backend.dto.UserDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.AdminUserRepository;
import com.societyportal.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> CSV_HEADERS =
            List.of("name", "flatNo", "block", "residentType", "mobile", "email");

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
                "Your City Apartment Portal account is ready",
                "An account has been created for you. Temporary password: " + password
                        + " - please log in and change it immediately.",
                "/login", true, null);
        return user;
    }

    /**
     * Each row is created and validated in its own transaction (note: no class-level
     * {@code @Transactional} here) so that one bad row - e.g. a genuine DB constraint violation
     * slipping past the pre-checks below - can't abort the whole batch. Postgres aborts an entire
     * transaction on the first constraint violation within it; sharing one transaction across all
     * rows would silently roll back every row already imported, and fail every row after it too,
     * the moment a single row conflicted.
     */
    public UserDtos.BulkImportResult bulkImport(org.springframework.web.multipart.MultipartFile csv) {
        List<String> errors = new ArrayList<>();
        int success = 0;
        // A real RFC 4180 parser (quoted fields with embedded commas/newlines, escaped quotes,
        // CRLF, headers matched case/order-insensitively) instead of a naive split(",") - names
        // like "Sharma, Rajesh" or "Doe Jr., James" are exactly the kind of ordinary data that
        // silently corrupted every column after it under the old line.split(",") approach.
        try (PushbackReader reader = new PushbackReader(new InputStreamReader(csv.getInputStream(), StandardCharsets.UTF_8))) {
            final int utf8Bom = 0xFEFF;
            int first = reader.read();
            if (first != utf8Bom && first != -1) reader.unread(first); // skip a UTF-8 BOM if present

            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true).setTrim(true)
                    .setIgnoreSurroundingSpaces(true)
                    .build();
            CSVParser parser = format.parse(reader);

            Set<String> headers = parser.getHeaderNames().stream().map(h -> h.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            List<String> missing = CSV_HEADERS.stream().filter(h -> !headers.contains(h.toLowerCase(Locale.ROOT))).toList();
            if (!missing.isEmpty()) {
                throw ApiException.badRequest("CSV is missing required column(s): " + String.join(", ", missing)
                        + ". Expected headers: " + String.join(",", CSV_HEADERS));
            }

            for (CSVRecord record : parser) {
                // +1 for the header row itself, +1 to make it 1-indexed for a human reading the CSV
                long rowNum = record.getRecordNumber() + 1;
                if (record.stream().allMatch(String::isBlank)) continue;
                try {
                    UserDtos.CreateUserRequest req = new UserDtos.CreateUserRequest();
                    req.setFullName(record.get("name"));
                    req.setFlatNo(record.get("flatNo"));
                    req.setBlock(record.get("block"));
                    req.setResidentType(record.get("residentType"));
                    req.setMobile(record.get("mobile"));
                    req.setEmail(record.get("email"));

                    // Must check admin_users too, not just users: login resolves an identifier
                    // against admin_users first (see AuthService.login), so an email/mobile that
                    // already belongs to an admin account would otherwise import a member account
                    // that can never actually log in.
                    if (userRepository.existsByEmailIgnoreCase(req.getEmail())
                            || adminUserRepository.existsByEmailIgnoreCase(req.getEmail())) {
                        errors.add("Row " + rowNum + ": email already exists (" + req.getEmail() + ")");
                        continue;
                    }
                    if (userRepository.existsByMobile(req.getMobile())
                            || adminUserRepository.existsByMobile(req.getMobile())) {
                        errors.add("Row " + rowNum + ": mobile already exists (" + req.getMobile() + ")");
                        continue;
                    }
                    createPreApproved(req);
                    success++;
                } catch (Exception e) {
                    log.warn("Bulk user import: row {} failed", rowNum, e);
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
                    "Your access to the City Apartment Portal has been suspended by the administrator.",
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
