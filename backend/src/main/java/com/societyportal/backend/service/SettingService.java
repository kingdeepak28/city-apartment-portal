package com.societyportal.backend.service;

import com.societyportal.backend.domain.Setting;
import com.societyportal.backend.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingRepository settingRepository;
    private final AuditService auditService;

    public Map<String, String> allSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        settingRepository.findAll().forEach(s -> map.put(s.getKey(), s.getValue()));
        return map;
    }

    public String get(String key, String defaultValue) {
        return settingRepository.findById(key).map(Setting::getValue).orElse(defaultValue);
    }

    @Transactional
    public void update(Map<String, String> updates) {
        updates.forEach((key, value) -> {
            Setting setting = settingRepository.findById(key)
                    .orElse(Setting.builder().key(key).build());
            setting.setValue(value);
            setting.setUpdatedAt(OffsetDateTime.now());
            settingRepository.save(setting);
        });
        auditService.log("SETTINGS", "UPDATE", null, null, updates);
    }
}
