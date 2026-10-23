package com.aiwatchspaces.ai_watch_spaces.repository;

import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpace;
import com.aiwatchspaces.ai_watch_spaces.entity.WatchSpaceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface WatchSpaceRepository extends JpaRepository<WatchSpace,Long> {
    List<WatchSpace> findByHostIdAndStatus(
            Long hostId,
            WatchSpaceStatus status
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WatchSpace w WHERE w.id = :id")
    Optional<WatchSpace> findByIdForUpdate(@Param("id") Long id);
    Optional<WatchSpace> findByInviteCode(String inviteCode);
}
