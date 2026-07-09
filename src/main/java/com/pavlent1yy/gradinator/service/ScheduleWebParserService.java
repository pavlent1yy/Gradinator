package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.enums.WeekType;
import com.pavlent1yy.gradinator.model.CellData;
import com.pavlent1yy.gradinator.model.Changes;
import com.pavlent1yy.gradinator.model.PairSlot;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import static com.pavlent1yy.gradinator.enums.WeekType.NUMERATOR;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@AllArgsConstructor
public class ScheduleWebParserService {

    private final WeekService weekService;

    public Changes getChanges(String group) {
        WeekType weekType = weekService.getWeekType();
        LocalDate date = null;
        List<PairSlot> changedPairs = new ArrayList<>();
        String[] urls = {"https://menu.sttec.yar.ru/timetable/rasp_second.html", "https://menu.sttec.yar.ru/timetable/rasp_first.html"};
        try {
            for (String url : urls) {
                Document document = Jsoup.connect(url).get();
                date = getDate(document);

                var table = document.body().select("div").select("table").get(0);
                if (!table.toString().contains(group)) {
                    continue;
                }

                var rows = table.select("tbody").select("tr");
                for (var row : rows) {
                    if (row.select("td").get(1).text().equalsIgnoreCase(group)) {
                        // Обрезаем пробелы сразу при получении текста из HTML
                        String pairNumber = row.select("td").get(2).text().trim();
                        String subject = row.select("td").get(4).text().trim();
                        String room = row.select("td").get(5).text().trim();

                        if (pairNumber.contains(",")) {
                            List<Integer> numbers = Arrays.stream(pairNumber.split(",")).map(Integer::parseInt).toList();
                            for (Integer changedPairNumber : numbers) {
                                changedPairs.add(buildPairSlot(changedPairNumber, subject, room, weekType));
                            }
                        }
                        else if (pairNumber.contains("-")) {
                            String[] numberArray = pairNumber.split("-");
                            int start = Integer.parseInt(numberArray[0].trim());
                            int end = Integer.parseInt(numberArray[1].trim());
                            for (int i = start; i <= end; i++) {
                                changedPairs.add(buildPairSlot(i, subject, room, weekType));
                            }
                        }
                        else {
                            changedPairs.add(buildPairSlot(Integer.parseInt(pairNumber), subject, room, weekType));
                        }

                    }
                }
            }

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new Changes(date,changedPairs);
    }

    private LocalDate getDate(Document document) {
        Pattern pattern = Pattern.compile("(\\d{1,2}\\s+[А-Яа-яЁё]+\\s+\\d{4})");

        Matcher matcher = pattern.matcher(document.text());

        if (!matcher.find()) {
            throw new IllegalStateException("Дата изменений не найдена");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "d MMMM yyyy",
                new Locale("ru", "RU")
        );

        return LocalDate.parse(matcher.group(1), formatter);
    }

    private static @NonNull PairSlot buildPairSlot(Integer changedPairNumber, String subject, String room, WeekType weekType) {
        PairSlot pairSlot = new PairSlot();
        pairSlot.setPairNumber(changedPairNumber);
        if (subject.trim().equalsIgnoreCase("снято")) {
            subject = "❕ Снято";
        } else
            subject = "❗ " + subject;
        CellData cell = new CellData(subject, room, "в предмете");

        if (weekType == NUMERATOR)
            pairSlot.setNumerator(cell);
        else
            pairSlot.setDenominator(cell);

        return pairSlot;
    }




}
