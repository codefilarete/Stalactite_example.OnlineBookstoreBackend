package com.switix.onlinebookstore.repository;

import java.util.List;

import com.switix.onlinebookstore.dto.CategoryBookCountDto;
import com.switix.onlinebookstore.model.Category;
import org.codefilarete.stalactite.spring.repository.StalactiteRepository;

public interface StalactiteCategoryRepository extends StalactiteRepository<Category,Long> {

    List<Category> findAllByNameLikeIgnoreCase(String category);

    // is defined by CategoryRepositoryTest.TestDataSourceConfig.countBooksByCategory(..)
    List<CategoryBookCountDto> countBooksByCategory();
}
