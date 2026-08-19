// Author: deepak.maheshwari

package com.societyportal.backend.repository;

import com.societyportal.backend.domain.AdminUser;
import com.societyportal.backend.domain.enums.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByEmailIgnoreCase(String email);

    Optional<AdminUser> findByEmailIgnoreCaseOrMobile(String email, String mobile);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

    long countByRole(AdminRole role);
}
