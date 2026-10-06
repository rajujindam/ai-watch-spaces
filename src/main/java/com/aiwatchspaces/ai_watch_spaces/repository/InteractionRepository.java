package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    List<Interaction> findByUserId(Long userId);

    List<Interaction> findByTitleId(Long titleId);

    List<Interaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Interaction> findByUserIdNot(Long userId);
}