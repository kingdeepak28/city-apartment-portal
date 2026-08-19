// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.OtpToken;
import com.societyportal.backend.domain.enums.OtpPurpose;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.otp.ttl-minutes}")
    private long ttlMinutes;

    @Transactional
    public void issueAndSend(String contact, OtpPurpose purpose) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpToken token = OtpToken.builder()
                .contact(contact)
                .purpose(purpose)
                .otpCode(otp)
                .verified(false)
                .expiresAt(OffsetDateTime.now().plusMinutes(ttlMinutes))
                .build();
        otpTokenRepository.save(token);

        String message = "Your City Apartments Portal OTP is " + otp + ". It is valid for " + ttlMinutes + " minutes.";
        if (contact.contains("@")) {
            emailService.send(contact, "Your verification OTP", message);
        } else {
            smsService.send(contact, message);
        }
    }

    @Transactional
    public void verify(String contact, OtpPurpose purpose, String otp) {
        OtpToken token = otpTokenRepository.findTopByContactAndPurposeOrderByCreatedAtDesc(contact, purpose)
                .orElseThrow(() -> ApiException.badRequest("No OTP was requested for this contact"));
        if (token.isVerified()) {
            return; // idempotent
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.badRequest("This OTP has expired, please request a new one");
        }
        if (!token.getOtpCode().equals(otp)) {
            throw ApiException.badRequest("Incorrect OTP");
        }
        token.setVerified(true);
        otpTokenRepository.save(token);
    }

    public boolean isVerified(String contact, OtpPurpose purpose) {
        return otpTokenRepository.findTopByContactAndPurposeOrderByCreatedAtDesc(contact, purpose)
                .map(OtpToken::isVerified)
                .orElse(false);
    }
}
