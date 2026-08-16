package com.societyportal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class CategoryDtos {

    @Data
    public static class CategoryRequest {
        @NotBlank
        private String type; // REPORT / NOTICE / PHOTO / MEETING / TENDER
        @NotBlank
        private String name;
        private UUID parentId;
        private Integer displayOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private UUID id;
        private String type;
        private String name;
        private UUID parentId;
        private String parentName;
        private int displayOrder;
        private boolean active;
        private long documentCount;
    }
}
