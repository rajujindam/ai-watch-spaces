package com.aiwatchspaces.ai_watch_spaces.service;

import com.aiwatchspaces.ai_watch_spaces.dto.CreateVariationOptionRequest;
import com.aiwatchspaces.ai_watch_spaces.dto.VariationOptionResponse;
import com.aiwatchspaces.ai_watch_spaces.entity.TimelineEvent;
import com.aiwatchspaces.ai_watch_spaces.entity.VariationOption;
import com.aiwatchspaces.ai_watch_spaces.exception.TimelineEventNotFoundException;
import com.aiwatchspaces.ai_watch_spaces.repository.TimelineEventRepository;
import com.aiwatchspaces.ai_watch_spaces.repository.VariationOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VariationOptionService {

    private final VariationOptionRepository variationOptionRepository;
    private final TimelineEventRepository timelineEventRepository;

    @Transactional
    public VariationOptionResponse createOption(
            Long timelineEventId,
            CreateVariationOptionRequest request
    ) {
        TimelineEvent timelineEvent =
                timelineEventRepository.findById(timelineEventId)
                        .orElseThrow(() ->
                                new TimelineEventNotFoundException(
                                        "Timeline event not found with id: "
                                                + timelineEventId
                                )
                        );

        if (!"variation_point".equals(timelineEvent.getEventType())) {
            throw new IllegalArgumentException(
                    "Timeline event is not a variation point"
            );
        }

        VariationOption option = new VariationOption(
                timelineEvent,
                request.label(),
                request.assetRef()
        );

        VariationOption savedOption =
                variationOptionRepository.save(option);

        return toResponse(savedOption);
    }

    @Transactional(readOnly = true)
    public List<VariationOptionResponse> getOptions(
            Long timelineEventId
    ) {
        TimelineEvent timelineEvent =
                timelineEventRepository.findById(timelineEventId)
                        .orElseThrow(() ->
                                new TimelineEventNotFoundException(
                                        "Timeline event not found with id: "
                                                + timelineEventId
                                )
                        );

        if (!"variation_point".equals(timelineEvent.getEventType())) {
            throw new IllegalArgumentException(
                    "Timeline event is not a variation point"
            );
        }

        return variationOptionRepository
                .findByTimelineEvent_Id(timelineEventId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VariationOptionResponse toResponse(
            VariationOption option
    ) {
        return new VariationOptionResponse(
                option.getId(),
                option.getTimelineEvent().getId(),
                option.getLabel(),
                option.getAssetRef(),
                option.getVoteCount()
        );
    }
}