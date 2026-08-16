package com.societyportal.backend.service;

import com.societyportal.backend.domain.Document;
import com.societyportal.backend.domain.enums.ContentType;
import com.societyportal.backend.domain.enums.DocumentStatus;
import com.societyportal.backend.dto.DocumentDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DocumentService documentService;

    @Transactional
    public Document create(DocumentDtos.ReportUpsertRequest req) {
        Document doc = documentService.createDraft(ContentType.REPORT, req.getTitle(), req.getCategoryId(),
                req.getSubCategoryId(), req.getDescription(), req.getTags(), metadataOf(req),
                req.getVisibilityType(), req.getVisibilityBlocks(), req.getVisibilityUserIds(),
                req.getPublishAt(), null, false);
        if ("PUBLISHED".equalsIgnoreCase(req.getStatus())) {
            documentService.publish(doc.getId());
        }
        return doc;
    }

    @Transactional
    public Document update(UUID id, DocumentDtos.ReportUpsertRequest req) {
        Document doc = documentService.update(id, req.getTitle(), req.getCategoryId(), req.getSubCategoryId(),
                req.getDescription(), req.getTags(), metadataOf(req),
                req.getVisibilityType(), req.getVisibilityBlocks(), req.getVisibilityUserIds(),
                req.getPublishAt(), null, false);
        if ("PUBLISHED".equalsIgnoreCase(req.getStatus()) && doc.getStatus() != DocumentStatus.PUBLISHED) {
            documentService.publish(id);
        }
        return doc;
    }

    private Map<String, Object> metadataOf(DocumentDtos.ReportUpsertRequest req) {
        Map<String, Object> m = new HashMap<>();
        m.put("financialYear", req.getFinancialYear());
        m.put("reportPeriod", req.getReportPeriod());
        m.put("preparedBy", req.getPreparedBy());
        m.put("reportDate", req.getReportDate() != null ? req.getReportDate().toString() : null);
        return m;
    }

    public Page<DocumentDtos.DocumentListItem> listForAdmin(UUID categoryId, String status, String keyword,
                                                              LocalDate from, LocalDate to, Pageable pageable) {
        return documentService.listForAdmin(ContentType.REPORT, categoryId, status, keyword, from, to, pageable)
                .map(this::toListItem);
    }

    public Page<DocumentDtos.DocumentListItem> listForMember(UUID categoryId, String keyword,
                                                               LocalDate from, LocalDate to, Pageable pageable) {
        return documentService.listForMember(ContentType.REPORT, categoryId, keyword, from, to, pageable)
                .map(this::toListItem);
    }

    public DocumentDtos.DocumentDetail detail(Document doc) {
        Map<String, Object> md = doc.getMetadata();
        return DocumentDtos.DocumentDetail.builder()
                .id(doc.getId()).contentType(doc.getContentType().name()).title(doc.getTitle())
                .categoryId(doc.getCategory() != null ? doc.getCategory().getId() : null)
                .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                .subCategoryId(doc.getSubCategory() != null ? doc.getSubCategory().getId() : null)
                .subCategoryName(doc.getSubCategory() != null ? doc.getSubCategory().getName() : null)
                .description(doc.getDescription()).tags(doc.getTags()).status(doc.getStatus().name())
                .visibilityType(doc.getVisibilityType().name())
                .visibilityBlocks(doc.getVisibilityBlocks().stream().toList())
                .visibilityUserIds(doc.getVisibilityUserIds().stream().toList())
                .publishAt(doc.getPublishAt()).publishedOn(doc.getPublishedOn())
                .createdByName(doc.getCreatedBy() != null ? doc.getCreatedBy().getName() : null)
                .createdAt(doc.getCreatedAt()).updatedAt(doc.getUpdatedAt())
                .viewCount(doc.getViewCount()).downloadCount(doc.getDownloadCount())
                .files(documentService.currentFiles(doc.getId()).stream().map(documentService::toFileInfo).toList())
                .financialYear(str(md.get("financialYear"))).reportPeriod(str(md.get("reportPeriod")))
                .reportDate(md.get("reportDate") != null ? LocalDate.parse(md.get("reportDate").toString()) : null)
                .preparedBy(str(md.get("preparedBy")))
                .build();
    }

    private DocumentDtos.DocumentListItem toListItem(Document doc) {
        Map<String, Object> md = doc.getMetadata();
        return DocumentDtos.DocumentListItem.builder()
                .id(doc.getId()).contentType(doc.getContentType().name()).title(doc.getTitle())
                .categoryName(doc.getCategory() != null ? doc.getCategory().getName() : null)
                .subCategoryName(doc.getSubCategory() != null ? doc.getSubCategory().getName() : null)
                .status(doc.getStatus().name()).visibilityType(doc.getVisibilityType().name())
                .tags(doc.getTags()).publishedOn(doc.getPublishedOn()).publishAt(doc.getPublishAt())
                .createdAt(doc.getCreatedAt())
                .fileCount(documentService.currentFiles(doc.getId()).size())
                .viewCount(doc.getViewCount()).downloadCount(doc.getDownloadCount())
                .financialYear(str(md.get("financialYear"))).reportPeriod(str(md.get("reportPeriod")))
                .reportDate(md.get("reportDate") != null ? LocalDate.parse(md.get("reportDate").toString()) : null)
                .preparedBy(str(md.get("preparedBy")))
                .build();
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}
