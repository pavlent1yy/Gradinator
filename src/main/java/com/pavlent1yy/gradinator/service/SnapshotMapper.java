package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import com.pavlent1yy.gradinator.entity.ScheduleSnapshot;
import com.pavlent1yy.gradinator.model.DaySchedule;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.PairSlot;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
public class SnapshotMapper {

    /** Сlaude AI:
     * Детерминированный hash по всем группам за день: сортируем группы,
     * внутри группы — пары по номеру, склеиваем в строку и берём SHA-256.
     * Порядок важен — без сортировки одинаковые данные могут дать разный hash.
     */
    public String computeHash(Map<String, DaySchedule> byGroup) {
        StringBuilder sb = new StringBuilder();

        byGroup.keySet().stream().sorted().forEach(group -> {
            sb.append(group).append('|');
            DaySchedule day = byGroup.get(group);
            for (PairSlot pair : day.getPairs().stream()
                    .sorted(Comparator.comparingInt(PairSlot::getPairNumber)).toList()) {
                sb.append(pair.getPairNumber()).append(':');
                appendCell(sb, pair.getNumerator());
                sb.append('/');
                appendCell(sb, pair.getDenominator());
                sb.append(';');
            }
            sb.append('\n');
        });

        return sha256(sb.toString());
    }

    private void appendCell(StringBuilder sb, CellData cell) {
        if (cell == null) return;
        sb.append(String.join(",", cell.getSubjects()))
                .append(String.join(",", cell.getTeachers()))
                .append(String.join(",", cell.getRooms()));
    }

    public List<ScheduleEntry> toEntries(ScheduleSnapshot snapshot, Map<String, DaySchedule> byGroup,
                                         Map<String, Set<Integer>> changedPairsByGroup) {
        List<ScheduleEntry> entries = new ArrayList<>();

        for (var e : byGroup.entrySet()) {
            String group = e.getKey();
            DaySchedule day = e.getValue();
            Set<Integer> changedPairs = changedPairsByGroup.getOrDefault(group, Set.of());

            for (PairSlot pair : day.getPairs()) {
                CellData num = pair.getNumerator();
                CellData den = pair.getDenominator();

                entries.add(ScheduleEntry.builder()
                        .snapshot(snapshot)
                        .groupName(group)
                        .day(day.getDay())
                        .pairNumber(pair.getPairNumber())
                        .numeratorSubjects(num != null ? num.getSubjects() : List.of())
                        .numeratorTeachers(num != null ? num.getTeachers() : List.of())
                        .numeratorRooms(num != null ? num.getRooms() : List.of())
                        .denominatorSubjects(den != null ? den.getSubjects() : List.of())
                        .denominatorTeachers(den != null ? den.getTeachers() : List.of())
                        .denominatorRooms(den != null ? den.getRooms() : List.of())
                        .hasChanges(changedPairs.contains(pair.getPairNumber()))
                        .build());
            }
        }

        return entries;
    }

    private List<String> collect(PairSlot pair) {
        List<String> subjects = new ArrayList<>();
        if (pair.getNumerator() != null) subjects.addAll(pair.getNumerator().getSubjects());
        if (pair.getDenominator() != null) subjects.addAll(pair.getDenominator().getSubjects());
        return subjects;
    }

    private List<String> collectTeachers(PairSlot pair) {
        List<String> teachers = new ArrayList<>();
        if (pair.getNumerator() != null) teachers.addAll(pair.getNumerator().getTeachers());
        if (pair.getDenominator() != null) teachers.addAll(pair.getDenominator().getTeachers());
        return teachers;
    }

    private List<String> collectRooms(PairSlot pair) {
        List<String> rooms = new ArrayList<>();
        if (pair.getNumerator() != null) rooms.addAll(pair.getNumerator().getRooms());
        if (pair.getDenominator() != null) rooms.addAll(pair.getDenominator().getRooms());
        return rooms;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}