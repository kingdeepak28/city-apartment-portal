package com.societyportal.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.societyportal.backend.domain.AuditLog;
import com.societyportal.backend.domain.enums.ActorType;
import com.societyportal.backend.repository.AuditLogRepository;
import com.societyportal.backend.security.AuthPrincipal;
import com.societyportal.backend.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /** Runs in its own transaction so an audit-write failure never rolls back the business action it is logging. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, String recordId, Object oldValue, Object newValue) {
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .module(module)
                .action(action)
                .recordId(recordId)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .ip(currentIp());

        try {
            AuthPrincipal principal = CurrentUser.get();
            builder.actorId(principal.getId())
                    .actorType(principal.isAdmin() ? ActorType.ADMIN : ActorType.MEMBER)
                    .actorName(principal.getName());
        } catch (IllegalStateException e) {
            builder.actorType(ActorType.SYSTEM).actorName("system");
        }

        auditLogRepository.save(builder.build());
    }

    /** Renders any loggable value (string/enum/map/...) as a JSON string, so the stored text is
     *  always self-describing and consistently quoted regardless of the caller's value type. */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Could not serialize audit log value, falling back to toString(): {}", e.getMessage());
            return String.valueOf(value);
        }
    }

    private String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
