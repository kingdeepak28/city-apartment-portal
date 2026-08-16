package com.societyportal.backend.repository;

import com.societyportal.backend.domain.User;
import com.societyportal.backend.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findByEmailIgnoreCaseOrMobile(String email, String mobile);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

    boolean existsByFlatNoIgnoreCaseAndBlockIgnoreCaseAndStatusIn(String flatNo, String block, List<UserStatus> statuses);

    long countByStatus(UserStatus status);

    List<User> findTop5ByStatusOrderByRegisteredOnDesc(UserStatus status);

    long countByStatusAndRegisteredOnBefore(UserStatus status, OffsetDateTime before);

    List<User> findByStatus(UserStatus status);

    List<User> findByBlockAndStatus(String block, UserStatus status);
}
