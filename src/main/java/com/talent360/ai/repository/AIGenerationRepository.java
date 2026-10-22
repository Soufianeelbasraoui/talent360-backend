package com.talent360.ai.repository;

import com.talent360.ai.entity.AIGeneration;
import com.talent360.common.enums.AIGenerationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIGenerationRepository extends JpaRepository<AIGeneration, Long> {
    List<AIGeneration> findByType(AIGenerationType type);
    List<AIGeneration> findByCreatedById(Long userId);
}
