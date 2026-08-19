// Author: deepak.maheshwari

package com.societyportal.backend.scheduler;

import com.societyportal.backend.domain.enums.ContentType;
import com.societyportal.backend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Two small housekeeping jobs required by the spec:
 *  - FR-AD-42 / FR-US-30: notices past their expiry date move automatically to Archived.
 *  - FR-AD-14: scheduled publishing - drafts whose publishAt time has arrived go live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentLifecycleScheduler {

    private final NoticeService noticeService;

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void archiveExpiredNotices() {
        int archived = noticeService.archiveExpired();
        if (archived > 0) {
            log.info("Archived {} expired notice(s)", archived);
        }
    }

    @Scheduled(fixedDelayString = "PT2M", initialDelayString = "PT30S")
    public void publishScheduledContent() {
        int reports = noticeService.publishScheduled(ContentType.REPORT);
        int notices = noticeService.publishScheduled(ContentType.NOTICE);
        if (reports + notices > 0) {
            log.info("Published {} scheduled report(s) and {} scheduled notice(s)", reports, notices);
        }
    }
}
