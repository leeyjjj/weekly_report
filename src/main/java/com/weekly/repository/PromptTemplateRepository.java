package com.weekly.repository;

import com.weekly.entity.PromptTemplate;
import com.weekly.entity.PromptType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    List<PromptTemplate> findAllByOrderByPromptTypeAscNameAsc();

    List<PromptTemplate> findByPromptTypeOrderByNameAsc(PromptType promptType);

    Optional<PromptTemplate> findByPromptTypeAndIsDefaultTrue(PromptType promptType);

    long countByPromptType(PromptType promptType);
}
