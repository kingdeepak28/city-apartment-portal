package com.societyportal.backend.repository;

import com.societyportal.backend.domain.OtpToken;
import com.societyportal.backend.domain.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findTopByContactAndPurposeOrderByCreatedAtDesc(String contact, OtpPurpose purpose);
}
