// Author: deepak.maheshwari

package com.societyportal.backend.controller;

import com.societyportal.backend.domain.enums.CategoryType;
import com.societyportal.backend.dto.CategoryDtos;
import com.societyportal.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDtos.CategoryResponse> list(@RequestParam String type,
                                                      @RequestParam(defaultValue = "false") boolean activeOnly) {
        return categoryService.list(CategoryType.valueOf(type.toUpperCase()), activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CategoryDtos.CategoryResponse create(@Valid @RequestBody CategoryDtos.CategoryRequest req) {
        return categoryService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public CategoryDtos.CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryDtos.CategoryRequest req) {
        return categoryService.update(id, req);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        categoryService.setActive(id, active);
        return ResponseEntity.ok(Map.of("message", "Updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted"));
    }
}
