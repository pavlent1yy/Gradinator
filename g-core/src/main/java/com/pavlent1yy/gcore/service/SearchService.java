package com.pavlent1yy.gcore.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pavlent1yy.gcore.client.GApiClient;
import com.pavlent1yy.gcore.dto.records.CellData;
import com.pavlent1yy.gcore.dto.records.FreeRoomsResponse;
import com.pavlent1yy.gcore.dto.records.PairResponse;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.dto.records.SearchHit;
import com.pavlent1yy.gcore.enums.SearchType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class SearchService {

    static final int MIN_QUERY_LENGTH = 2;
    static final int MAX_HITS = 300;

    private final GApiClient gApiClient;
    private final Cache<LocalDate, Map<String, ScheduleResponse>> daySchedules;
    private final Cache<String, List<String>> dictionaries;

    public SearchService(GApiClient gApiClient) {
        this.gApiClient = gApiClient;
        this.daySchedules = Caffeine.newBuilder()
                .maximumSize(30)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
        this.dictionaries = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

    public List<SearchHit> search(String query, SearchType type, LocalDate date) {
        String needle = normalize(query);
        if (needle.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        SearchType searchType = type == null ? SearchType.ANY : type;
        List<SearchHit> hits = new ArrayList<>();

        allSchedules(date).forEach((group, schedule) -> pairs(schedule).forEach(pair -> {
            CellData cell = ScheduleCells.activeCell(pair, schedule.weekType());
            if (cell != null && matches(cell, needle, searchType)) {
                hits.add(new SearchHit(
                        group,
                        pair.pairNumber(),
                        orEmpty(cell.subjects()),
                        orEmpty(cell.teachers()),
                        orEmpty(cell.rooms()),
                        pair.hasChanges()
                ));
            }
        }));

        return hits.stream()
                .sorted(Comparator.comparingInt(SearchHit::pairNumber).thenComparing(SearchHit::group))
                .limit(MAX_HITS)
                .toList();
    }

    public List<FreeRoomsResponse> freeRooms(LocalDate date) {
        Map<String, ScheduleResponse> schedules = allSchedules(date);
        if (schedules.isEmpty()) {
            return List.of();
        }

        Map<Integer, Set<String>> busyByPair = new TreeMap<>();
        Set<String> knownRooms = new TreeSet<>();

        schedules.values().forEach(schedule -> pairs(schedule).forEach(pair -> {
            CellData cell = ScheduleCells.activeCell(pair, schedule.weekType());
            if (cell == null) {
                return;
            }
            Set<String> busy = busyByPair.computeIfAbsent(pair.pairNumber(), n -> new TreeSet<>());
            splitRooms(cell.rooms()).forEach(room -> {
                busy.add(room);
                knownRooms.add(room);
            });
        }));

        return busyByPair.entrySet().stream()
                .map(entry -> new FreeRoomsResponse(
                        entry.getKey(),
                        knownRooms.stream().filter(room -> !entry.getValue().contains(room)).toList(),
                        List.copyOf(entry.getValue())
                ))
                .toList();
    }

    public List<String> teachers() {
        return dictionary("teachers", gApiClient::getTeachers);
    }

    public List<String> subjects() {
        return dictionary("subjects", gApiClient::getSubjects);
    }

    public List<String> rooms() {
        return dictionary("rooms", () -> gApiClient.getRooms().stream()
                .flatMap(room -> splitRooms(List.of(room)))
                .toList());
    }

    private Map<String, ScheduleResponse> allSchedules(LocalDate date) {
        return daySchedules.get(date, gApiClient::getAllSchedules);
    }

    private List<String> dictionary(String key, Supplier<List<String>> loader) {
        return dictionaries.get(key, k -> cleanDictionary(loader.get()));
    }

    static List<String> cleanDictionary(Collection<String> values) {
        return values.stream()
                .map(value -> value == null ? "" : value.trim().replaceAll("\\s+", " "))
                .filter(value -> value.length() >= 2 && value.chars().anyMatch(Character::isLetterOrDigit))
                .collect(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER), TreeSet::add, TreeSet::addAll)
                .stream()
                .toList();
    }

    private static Stream<String> splitRooms(List<String> rooms) {
        return orEmpty(rooms).stream()
                .flatMap(room -> Arrays.stream(room.split("[,;]")))
                .map(String::trim)
                .filter(room -> !room.isEmpty());
    }

    private static boolean matches(CellData cell, String needle, SearchType type) {
        return switch (type) {
            case TEACHER -> containsIn(cell.teachers(), needle);
            case SUBJECT -> containsIn(cell.subjects(), needle);
            case ROOM -> containsIn(cell.rooms(), needle);
            case ANY -> containsIn(cell.teachers(), needle)
                    || containsIn(cell.subjects(), needle)
                    || containsIn(cell.rooms(), needle);
        };
    }

    private static boolean containsIn(List<String> values, String needle) {
        return orEmpty(values).stream().anyMatch(value -> normalize(value).contains(needle));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("\\s+", " ");
    }

    private static List<PairResponse> pairs(ScheduleResponse schedule) {
        return schedule == null || schedule.pairs() == null ? List.of() : schedule.pairs();
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
