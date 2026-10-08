package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpace;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface WatchSpaceRepository extends JpaRepository<WatchSpace,Long> {
    List<WatchSpace> findByHostIdAndStatus(
            Long hostId,
            WatchSpaceStatus status
    );
}
