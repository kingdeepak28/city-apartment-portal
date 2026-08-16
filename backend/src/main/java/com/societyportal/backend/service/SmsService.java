package com.societyportal.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS gateway integration point. Per the spec's own open question #5, SMS needs a
 * DLT-registered sender ID and approved templates before it can go live in India,
 * so this defaults to log-only and can be wired to a real provider (e.g. an SMS
 * aggregator's REST API) by flipping {@code app.sms.enabled} and filling in the
 * provider call in {@link #send}.
 */
@Service
@Slf4j
public class SmsService {

    private final boolean enabled;

    public SmsService(@Value("${app.sms.enabled}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean send(String mobile, String message) {
        if (!enabled) {
            log.info("[SMS:DEV-MODE] to={} message={}", mobile, message);
            return true;
        }
        // TODO: integrate a DLT-registered SMS provider here.
        log.warn("SMS sending is enabled but no provider is wired up yet; message to {} was not sent", mobile);
        return false;
    }
}
