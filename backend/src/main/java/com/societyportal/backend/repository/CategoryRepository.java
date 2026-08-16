package com.societyportal.backend.repository;

import com.societyportal.backend.domain.Category;
import com.societyportal.backend.domain.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByTypeOrderByDisplayOrderAsc(CategoryType type);

    List<Category> findByTypeAndActiveTrueOrderByDisplayOrderAsc(CategoryType type);

    List<Category> findByParentId(UUID parentId);

    boolean existsByTypeAndNameIgnoreCaseAndParentId(CategoryType type, String name, UUID parentId);
}
