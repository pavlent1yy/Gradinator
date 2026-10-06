package com.pavlent1yy.gcore.dto.records;

import java.util.List;

public record FreeRoomsResponse(
        int pairNumber,
        List<String> freeRooms,
        List<String> busyRooms
) {}
