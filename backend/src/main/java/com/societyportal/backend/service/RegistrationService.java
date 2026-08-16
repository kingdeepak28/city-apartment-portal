package com.societyportal.backend.service;

import com.societyportal.backend.domain.AdminUser;
import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.NotificationType;
import com.societyportal.backend.domain.enums.ResidentType;
import com.societyportal.backend.domain.enums.UserStatus;
import com.societyportal.backend.dto.RegistrationDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.AdminUserRepository;
import com.societyportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final List<UserStatus> ACTIVE_LIKE_STATUSES =
            List.of(UserStatus.PENDING, UserStatus.ACTIVE, UserStatus.INFO_REQUESTED);

    public void checkDuplicate(String email, String mobile, String flatNo, String block) {
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        if (mobile != null && userRepository.existsByMobile(mobile)) {
            throw ApiException.conflict("An account with this mobile number already exists");
        }
        if (flatNo != null && block != null
                && userRepository.existsByFlatNoIgnoreCaseAndBlockIgnoreCaseAndStatusIn(flatNo, block, ACTIVE_LIKE_STATUSES)) {
            throw ApiException.conflict("An active or pending account already exists for flat " + flatNo + ", block " + block);
        }
    }

    @Transactional
    public RegistrationDtos.RegistrationSubmittedResponse submit(RegistrationDtos.RegisterRequest req, MultipartFile proof) {
        checkDuplicate(req.getEmail(), req.getMobile(), req.getFlatNo(), req.getBlock());

        if (proof == null || proof.isEmpty()) {
            throw ApiException.badRequest("ID/ownership proof document is required");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(proof, "registration-proofs");

        User user = User.builder()
                .name(req.getFullName())
                .email(req.getEmail())
                .mobile(req.getMobile())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .flatNo(req.getFlatNo())
                .block(req.getBlock())
                .residentType(ResidentType.valueOf(req.getResidentType().toUpperCase()))
                .status(UserStatus.PENDING)
                .proofFilePath(stored.relativePath())
                // OTP verification is disabled for self-registration - email/mobile are only
                // established as genuine at admin-approval time, not by this self-reported form.
                .emailVerified(false)
                .mobileVerified(false)
                .build();
        user = userRepository.save(user);

        auditService.log("REGISTRATION", "SUBMITTED", user.getId().toString(), null, null);

        List<AdminUser> admins = adminUserRepository.findAll();
        notificationService.notifyAdmins(admins, NotificationType.REGISTRATION_SUBMITTED,
                "New registration: " + user.getName() + " (Flat " + user.getFlatNo() + ")",
                user.getName() + " has submitted a registration request and is awaiting approval.",
                "/admin/approvals/" + user.getId());

        return new RegistrationDtos.RegistrationSubmittedResponse(user.getId(), "PENDING",
                "Your registration has been submitted and is awaiting approval by the society administrator.");
    }
}
