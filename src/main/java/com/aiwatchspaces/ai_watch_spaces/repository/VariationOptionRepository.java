package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.VariationOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariationOptionRepository
        extends JpaRepository<VariationOption, Long> {

    List<VariationOption> findByTimelineEvent_Id(Long timelineEventId);
}