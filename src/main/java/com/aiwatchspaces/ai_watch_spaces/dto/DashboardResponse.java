package com.aiwatchspaces.ai_watch_spaces.dto;

import java.util.List;

public record DashboardResponse(
        List<TitleResponse> recentlyWatched,
        List<WatchSpaceResponse> upcomingWatchSpaces,
        List<WatchSpaceResponse> quickRejoin,
        List<RecommendationResponse> recommendations
) {
}