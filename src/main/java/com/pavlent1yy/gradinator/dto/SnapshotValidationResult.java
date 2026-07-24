package com.pavlent1yy.gradinator.dto;

import java.util.List;

public record SnapshotValidationResult(
        Long snapshotId,
        String scheduleDate,
        boolean hashMatches,
        List<String> missingGroups,
        List<String> groupsWithDuplicatePairs,
        int totalEntries
) {}