// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.AdminUser;
import com.societyportal.backend.domain.enums.AdminRole;
import com.societyportal.backend.domain.enums.AdminStatus;
import com.societyportal.backend.dto.AdminAccountDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.AdminUserRepository;
import com.societyportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AdminUserRepository adminUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailService emailService;
    private static final SecureRandom RANDOM = new SecureRandom();

    public List<AdminAccountDtos.AdminSummary> list() {
        return adminUserRepository.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional
    public AdminAccountDtos.AdminSummary create(AdminAccountDtos.CreateAdminRequest req) {
        // Must also check the member table, not just admin_users: login resolves an identifier
        // against admin_users first (see AuthService.login), so handing this email/mobile to a
        // new admin account here would silently strand any existing member account sharing it -
        // that member could never log in again, since the login form would always match the
        // admin account instead.
        if (adminUserRepository.existsByEmailIgnoreCase(req.getEmail())
                || userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        if (req.getMobile() != null && !req.getMobile().isBlank()
                && (adminUserRepository.existsByMobile(req.getMobile()) || userRepository.existsByMobile(req.getMobile()))) {
            throw ApiException.conflict("An account with this mobile number already exists");
        }
        String password = (req.getPassword() == null || req.getPassword().isBlank()) ? generateTempPassword() : req.getPassword();
        AdminUser admin = AdminUser.builder()
                .name(req.getName()).email(req.getEmail()).mobile(req.getMobile())
                .role(AdminRole.valueOf(req.getRole().toUpperCase()))
                .passwordHash(passwordEncoder.encode(password))
                .status(AdminStatus.ACTIVE)
                .build();
        admin = adminUserRepository.save(admin);
        auditService.log("ADMIN_ACCOUNT", "CREATE", admin.getId().toString(), null, req.getRole());
        emailService.send(admin.getEmail(), "Your City Apartment Portal admin account",
                "An admin account has been created for you. Temporary password: " + password + ". Please log in and change it immediately.");
        return toSummary(admin);
    }

    @Transactional
    public void updateRole(UUID id, String role) {
        AdminUser admin = adminUserRepository.findById(id).orElseThrow(() -> ApiException.notFound("Admin not found"));
        AdminRole old = admin.getRole();
        admin.setRole(AdminRole.valueOf(role.toUpperCase()));
        adminUserRepository.save(admin);
        auditService.log("ADMIN_ACCOUNT", "ROLE_CHANGE", id.toString(), old, role);
    }

    @Transactional
    public void updateStatus(UUID id, String status) {
        AdminUser admin = adminUserRepository.findById(id).orElseThrow(() -> ApiException.notFound("Admin not found"));
        AdminStatus old = admin.getStatus();
        admin.setStatus(AdminStatus.valueOf(status.toUpperCase()));
        adminUserRepository.save(admin);
        auditService.log("ADMIN_ACCOUNT", "STATUS_CHANGE", id.toString(), old, status);
    }

    @Transactional
    public void delete(UUID id) {
        adminUserRepository.deleteById(id);
        auditService.log("ADMIN_ACCOUNT", "DELETE", id.toString(), null, null);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    private AdminAccountDtos.AdminSummary toSummary(AdminUser a) {
        return new AdminAccountDtos.AdminSummary(a.getId(), a.getName(), a.getEmail(), a.getMobile(),
                a.getRole().name(), a.getStatus().name(), a.getLastLogin(), a.getCreatedAt());
    }
}
