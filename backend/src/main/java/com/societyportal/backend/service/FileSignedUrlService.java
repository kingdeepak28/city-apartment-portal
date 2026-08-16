package com.societyportal.backend.service;

import com.societyportal.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues short-lived, tamper-proof download tokens so a document file is
 * never reachable by a bare, permanent URL - only via a signed link minted
 * for an authenticated, authorised request (NFR: File security).
 */
@Service
public class FileSignedUrlService {

    private static final String ALGO = "HmacSHA256";

    private final byte[] secret;
    private final long ttlMinutes;

    public FileSignedUrlService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.file-storage.signed-url-ttl-minutes}") long ttlMinutes) {
        this.secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.ttlMinutes = ttlMinutes;
    }

    public String issueToken(UUID fileId, UUID requesterId) {
        long expiresAt = Instant.now().plusSeconds(ttlMinutes * 60).getEpochSecond();
        String payload = fileId + ":" + requesterId + ":" + expiresAt;
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
    }

    public UUID verifyAndGetFileId(String token) {
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw ApiException.forbidden("Invalid or expired download link");
        }
        String[] parts = decoded.split(":");
        if (parts.length != 4) {
            throw ApiException.forbidden("Invalid or expired download link");
        }
        String fileId = parts[0];
        String requesterId = parts[1];
        String expiresAt = parts[2];
        String signature = parts[3];
        String payload = fileId + ":" + requesterId + ":" + expiresAt;
        if (!sign(payload).equals(signature)) {
            throw ApiException.forbidden("Invalid or expired download link");
        }
        if (Instant.now().getEpochSecond() > Long.parseLong(expiresAt)) {
            throw ApiException.forbidden("This download link has expired, please try again");
        }
        return UUID.fromString(fileId);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret, ALGO));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign download token", e);
        }
    }
}
