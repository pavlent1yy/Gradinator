package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.PairSlot;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.pavlent1yy.gradinator.enums.WeekType.NUMERATOR;

@Slf4j
@Service
@AllArgsConstructor
public class ScheduleWebParserService {

    private final WeekService weekService;

    private static final String[] URLS = {
            "https://menu.sttec.yar.ru/timetable/rasp_second.html",
            "https://menu.sttec.yar.ru/timetable/rasp_first.html"
    };

    public record AllChanges(LocalDate date, Map<String, List<PairSlot>> byGroup) {

        public List<PairSlot> forGroup(String group) {
            return byGroup.getOrDefault(group, List.of());
        }
    }

    public AllChanges getAllChanges() {
        WeekType weekType = weekService.getWeekType();
        LocalDate date = null;
        Map<String, List<PairSlot>> byGroup = new HashMap<>();

        for (String url : URLS) {
            try {
                Document document = Jsoup.connect(url).get();
                LocalDate parsedDate = getDate(document);
                if (date == null) date = parsedDate;

                var table = document.body().select("div").select("table").get(0);
                var rows = table.select("tbody").select("tr");

                for (var row : rows) {
                    var cells = row.select("td");
                    if (cells.size() < 6) continue;

                    String group = cells.get(1).text().trim();
                    if (group.isBlank()) continue;

                    String pairNumber = cells.get(2).text().trim();
                    if (!pairNumber.matches("^[\\d,\\-\\s]+$")) {
                        log.debug("Пропускаю строку — не похоже на номер пары: '{}'", pairNumber);
                        continue;
                    }

                    String subject = cells.get(4).text().trim();
                    String room = cells.get(5).text().trim();

                    List<PairSlot> pairs = expandPairNumbers(pairNumber, subject, room, weekType);
                    byGroup.computeIfAbsent(group, k -> new ArrayList<>()).addAll(pairs);
                }
            } catch (Exception e) {
                log.error("Не удалось разобрать замены с {}", url, e);
            }
        }

        return new AllChanges(date, byGroup);
    }

    private List<PairSlot> expandPairNumbers(String pairNumber, String subject, String room, WeekType weekType) {
        List<PairSlot> result = new ArrayList<>();

        if (pairNumber.contains(",")) {
            for (String n : pairNumber.split(",")) {
                result.add(buildPairSlot(Integer.parseInt(n.trim()), subject, room, weekType));
            }
        } else if (pairNumber.contains("-")) {
            String[] parts = pairNumber.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            for (int i = start; i <= end; i++) {
                result.add(buildPairSlot(i, subject, room, weekType));
            }
        } else {
            result.add(buildPairSlot(Integer.parseInt(pairNumber), subject, room, weekType));
        }

        return result;
    }

    private LocalDate getDate(Document document) {
        Pattern pattern = Pattern.compile("(\\d{1,2}\\s+[А-Яа-яЁё]+\\s+\\d{4})");
        Matcher matcher = pattern.matcher(document.text());

        if (!matcher.find()) {
            throw new IllegalStateException("Дата изменений не найдена");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru", "RU"));
        return LocalDate.parse(matcher.group(1), formatter);
    }

    private static @NonNull PairSlot buildPairSlot(Integer pairNumber, String subject, String room, WeekType weekType) {
        PairSlot pairSlot = new PairSlot();
        pairSlot.setPairNumber(pairNumber);

        subject = subject.trim().equalsIgnoreCase("снято") ? "❕ Снято" : "❗ " + subject;
        CellData cell = new CellData(subject, room, "в предмете");

        if (weekType == NUMERATOR) pairSlot.setNumerator(cell);
        else pairSlot.setDenominator(cell);

        return pairSlot;
    }
}