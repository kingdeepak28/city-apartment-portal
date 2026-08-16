package com.societyportal.backend.service;

import com.societyportal.backend.domain.AdminUser;
import com.societyportal.backend.domain.PasswordResetToken;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.AccountType;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.domain.enums.UserStatus;
import com.societyportal.backend.dto.AuthDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.AdminUserRepository;
import com.societyportal.backend.repository.PasswordResetTokenRepository;
import com.societyportal.backend.repository.UserRepository;
import com.societyportal.backend.security.AuthPrincipal;
import com.societyportal.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${app.security.max-failed-login-attempts}")
    private int maxAttempts;

    @Value("${app.security.lockout-minutes}")
    private long lockoutMinutes;

    @Transactional
    public AuthDtos.LoginResponse login(String identifier, String password) {
        Optional<AdminUser> adminOpt = adminUserRepository.findByEmailIgnoreCaseOrMobile(identifier, identifier);
        if (adminOpt.isPresent()) {
            return loginAdmin(adminOpt.get(), password);
        }
        Optional<User> userOpt = userRepository.findByEmailIgnoreCaseOrMobile(identifier, identifier);
        if (userOpt.isPresent()) {
            return loginMember(userOpt.get(), password);
        }
        throw ApiException.unauthorized("Invalid email/mobile or password");
    }

    private AuthDtos.LoginResponse loginAdmin(AdminUser admin, String password) {
        assertNotLocked(admin.getLockedUntil());
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            registerFailure(admin);
            throw ApiException.unauthorized("Invalid email/mobile or password");
        }
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setLastLogin(OffsetDateTime.now());
        adminUserRepository.save(admin);

        AuthPrincipal principal = new AuthPrincipal(admin.getId(), admin.getEmail(), admin.getName(),
                AccountType.ADMIN, admin.getRole().name());
        String token = jwtService.generateToken(principal);
        auditService.log("AUTH", "LOGIN", admin.getId().toString(), null, null);
        return new AuthDtos.LoginResponse(token, admin.getId(), admin.getName(), admin.getEmail(),
                AccountType.ADMIN.name(), admin.getRole().name());
    }

    private AuthDtos.LoginResponse loginMember(User user, String password) {
        assertNotLocked(user.getLockedUntil());
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            registerFailure(user);
            throw ApiException.unauthorized("Invalid email/mobile or password");
        }
        switch (user.getStatus()) {
            case PENDING -> throw ApiException.forbidden("Your account is awaiting approval by the society administrator");
            case REJECTED -> throw ApiException.forbidden("Your registration was rejected: "
                    + (user.getRejectionReason() != null ? user.getRejectionReason() : "please contact the society office")
                    + ". You may re-apply.");
            case SUSPENDED -> throw ApiException.forbidden("Your account has been suspended. Please contact the society administrator");
            case INFO_REQUESTED -> throw ApiException.forbidden("Additional information was requested for your registration: "
                    + (user.getInfoRequestedNote() != null ? user.getInfoRequestedNote() : "please update and resubmit your application"));
            case ACTIVE -> { /* proceed */ }
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(OffsetDateTime.now());
        userRepository.save(user);

        AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getEmail(), user.getName(),
                AccountType.USER, "MEMBER");
        String token = jwtService.generateToken(principal);
        auditService.log("AUTH", "LOGIN", user.getId().toString(), null, null);
        return new AuthDtos.LoginResponse(token, user.getId(), user.getName(), user.getEmail(),
                AccountType.USER.name(), "MEMBER");
    }

    private void assertNotLocked(OffsetDateTime lockedUntil) {
        if (lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now())) {
            long minutesLeft = java.time.Duration.between(OffsetDateTime.now(), lockedUntil).toMinutes() + 1;
            throw ApiException.forbidden("Account temporarily locked after too many failed attempts. Try again in "
                    + minutesLeft + " minute(s) or contact an administrator");
        }
    }

    private void registerFailure(AdminUser admin) {
        int attempts = admin.getFailedLoginAttempts() + 1;
        admin.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            admin.setLockedUntil(OffsetDateTime.now().plusMinutes(lockoutMinutes));
        }
        adminUserRepository.save(admin);
    }

    private void registerFailure(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(lockoutMinutes));
        }
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String identifier) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCaseOrMobile(identifier, identifier);
        Optional<AdminUser> adminOpt = adminUserRepository.findByEmailIgnoreCaseOrMobile(identifier, identifier);

        if (userOpt.isEmpty() && adminOpt.isEmpty()) {
            return; // do not reveal whether the account exists
        }
        AccountType type = userOpt.isPresent() ? AccountType.USER : AccountType.ADMIN;
        UUID accountId = userOpt.map(User::getId).orElseGet(() -> adminOpt.get().getId());
        String email = userOpt.map(User::getEmail).orElseGet(() -> adminOpt.get().getEmail());

        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder()
                .accountType(type).accountId(accountId).token(token)
                .expiresAt(OffsetDateTime.now().plusMinutes(30)).used(false).build());

        String link = "/reset-password?token=" + token;
        emailService.send(email, "Reset your Society Document Portal password",
                "Click the link below to reset your password (valid for 30 minutes):<br/><a href=\"" + link + "\">" + link + "</a>");
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset link"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.badRequest("Invalid or expired reset link");
        }
        String hash = passwordEncoder.encode(newPassword);
        if (resetToken.getAccountType() == AccountType.USER) {
            User user = userRepository.findById(resetToken.getAccountId())
                    .orElseThrow(() -> ApiException.notFound("Account not found"));
            user.setPasswordHash(hash);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        } else {
            AdminUser admin = adminUserRepository.findById(resetToken.getAccountId())
                    .orElseThrow(() -> ApiException.notFound("Account not found"));
            admin.setPasswordHash(hash);
            admin.setFailedLoginAttempts(0);
            admin.setLockedUntil(null);
            adminUserRepository.save(admin);
        }
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    @Transactional
    public void changePassword(UUID userId, boolean isAdmin, String currentPassword, String newPassword) {
        if (isAdmin) {
            AdminUser admin = adminUserRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Account not found"));
            if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
                throw ApiException.badRequest("Current password is incorrect");
            }
            admin.setPasswordHash(passwordEncoder.encode(newPassword));
            adminUserRepository.save(admin);
        } else {
            User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Account not found"));
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw ApiException.badRequest("Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
    }

    @Transactional
    public void adminTriggerPasswordReset(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        forgotPassword(user.getEmail());
        auditService.log("USER", "ADMIN_TRIGGERED_PASSWORD_RESET", userId.toString(), null, null);
    }

    @Transactional
    public void unlockAccount(UUID userId, boolean isAdminAccount) {
        if (isAdminAccount) {
            AdminUser admin = adminUserRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Account not found"));
            admin.setFailedLoginAttempts(0);
            admin.setLockedUntil(null);
            adminUserRepository.save(admin);
        } else {
            User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Account not found"));
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        auditService.log("AUTH", "UNLOCK_ACCOUNT", userId.toString(), null, null);
    }
}
