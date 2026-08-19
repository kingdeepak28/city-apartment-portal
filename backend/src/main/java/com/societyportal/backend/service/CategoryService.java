// Author: deepak.maheshwari

package com.societyportal.backend.service;

import com.societyportal.backend.domain.Category;
import com.societyportal.backend.domain.enums.CategoryType;
import com.societyportal.backend.dto.CategoryDtos;
import com.societyportal.backend.exception.ApiException;
import com.societyportal.backend.repository.CategoryRepository;
import com.societyportal.backend.repository.DocumentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;

    public List<CategoryDtos.CategoryResponse> list(CategoryType type, boolean activeOnly) {
        List<Category> categories = activeOnly
                ? categoryRepository.findByTypeAndActiveTrueOrderByDisplayOrderAsc(type)
                : categoryRepository.findByTypeOrderByDisplayOrderAsc(type);
        return categories.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryRequest req) {
        CategoryType type = CategoryType.valueOf(req.getType().toUpperCase());
        if (categoryRepository.existsByTypeAndNameIgnoreCaseAndParentId(type, req.getName(), req.getParentId())) {
            throw ApiException.conflict("A category with this name already exists at this level");
        }
        Category parent = req.getParentId() != null
                ? categoryRepository.findById(req.getParentId()).orElseThrow(() -> ApiException.notFound("Parent category not found"))
                : null;
        if (parent != null && parent.getParent() != null) {
            throw ApiException.badRequest("Sub-categories can only be one level deep");
        }
        Category category = Category.builder()
                .type(type).name(req.getName()).parent(parent)
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .active(true)
                .build();
        category = categoryRepository.save(category);
        auditService.log("CATEGORY", "CREATE", category.getId().toString(), null, req.getName());
        return toResponse(category);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(UUID id, CategoryDtos.CategoryRequest req) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
        String oldName = category.getName();
        category.setName(req.getName());
        if (req.getDisplayOrder() != null) {
            category.setDisplayOrder(req.getDisplayOrder());
        }
        categoryRepository.save(category);
        auditService.log("CATEGORY", "UPDATE", id.toString(), oldName, req.getName());
        return toResponse(category);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
        category.setActive(active);
        categoryRepository.save(category);
        auditService.log("CATEGORY", active ? "ACTIVATE" : "DEACTIVATE", id.toString(), !active, active);
    }

    @Transactional
    public void delete(UUID id) {
        long mapped = countDocumentsInCategory(id);
        if (mapped > 0) {
            throw ApiException.conflict("This category has " + mapped + " document(s) mapped to it. Reassign them before deleting.");
        }
        categoryRepository.deleteById(id);
        auditService.log("CATEGORY", "DELETE", id.toString(), null, null);
    }

    private long countDocumentsInCategory(UUID categoryId) {
        Specification<com.societyportal.backend.domain.Document> spec = (root, query, cb) -> {
            Predicate p1 = cb.equal(root.get("category").get("id"), categoryId);
            Predicate p2 = cb.equal(root.get("subCategory").get("id"), categoryId);
            return cb.and(cb.equal(root.get("deleted"), false), cb.or(p1, p2));
        };
        return documentRepository.count(spec);
    }

    private CategoryDtos.CategoryResponse toResponse(Category c) {
        return new CategoryDtos.CategoryResponse(
                c.getId(), c.getType().name(), c.getName(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getParent() != null ? c.getParent().getName() : null,
                c.getDisplayOrder(), c.isActive(), countDocumentsInCategory(c.getId()));
    }
}
