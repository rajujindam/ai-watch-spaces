
package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchSpaceParticipantRepository
        extends JpaRepository<WatchSpaceParticipant, Long> {

    boolean existsByWatchSpace_IdAndUserId(Long watchSpaceId, Long userId);
    List<WatchSpaceParticipant> findByWatchSpace_Id(Long watchSpaceId);
    List<WatchSpaceParticipant> findByUserId(Long userId);
    long countByWatchSpace_Id(Long watchSpaceId);
    Optional<WatchSpaceParticipant> findByWatchSpace_IdAndUserId(
            Long watchSpaceId,
            Long userId
    );

}