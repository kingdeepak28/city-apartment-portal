package com.societyportal.backend.domain;

import com.societyportal.backend.domain.enums.ContentType;
import com.societyportal.backend.domain.enums.DocumentStatus;
import com.societyportal.backend.domain.enums.VisibilityType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Generic content container. Reports and Notices (Phase 1) are fully modelled
 * this way; type-specific fields (financial year, notice number, priority...)
 * are kept in {@link #metadata} so the same table serves every content type,
 * including the Phase 2 types (Photo albums, Minutes, Tenders) whose detail
 * tables already exist in the schema.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(nullable = false, length = 300)
    private String title;

    // EAGER: a single nullable to-one reference that every list/detail DTO needs, resolved via a
    // JOIN on load - unlike a collection, this doesn't risk N+1/cartesian-product blowups, and it
    // sidesteps LazyInitializationException when a Document outlives the transaction that loaded it
    // (open-in-view is disabled, so that happens on every "fetch here, map to DTO there" call).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_category_id")
    private Category subCategory;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 500)
    private String tags;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_type", nullable = false, length = 20)
    @Builder.Default
    private VisibilityType visibilityType = VisibilityType.ALL;

    // EAGER: only ever read one document at a time (never in a paginated list mapping), and it's a
    // handful of rows at most, so the extra per-entity query is cheap - and it avoids the same
    // LazyInitializationException risk as the to-one associations above.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_visibility_blocks", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "block")
    @Builder.Default
    private Set<String> visibilityBlocks = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_visibility_users", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "user_id")
    @Builder.Default
    private Set<UUID> visibilityUserIds = new HashSet<>();

    @Column(name = "publish_at")
    private OffsetDateTime publishAt;

    @Column(name = "published_on")
    private OffsetDateTime publishedOn;

    @Column(name = "expiry_at")
    private OffsetDateTime expiryAt;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private int downloadCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private AdminUser createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "updated_by")
    private AdminUser updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNo DESC")
    @Builder.Default
    private List<DocumentFile> files = new ArrayList<>();
}
