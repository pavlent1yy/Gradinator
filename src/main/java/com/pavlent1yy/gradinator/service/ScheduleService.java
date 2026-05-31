package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.dto.ScheduleProjection;
import com.pavlent1yy.gradinator.entity.User;
import com.pavlent1yy.gradinator.repository.ScheduleRepository;
import com.pavlent1yy.gradinator.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@AllArgsConstructor
public class ScheduleService {
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final CurrentWeekService getCurrentWeekService;
    private final ScheduleWebParserService updateScheduleHandler;
    private final WeekdaysConvertor weekdaysConvertor;

    public String getSchedule(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String currentDay = getCurrentWeekService.getCurrentDay();
        String groupName = user.getStudentGroup().getName();
        long groupId = user.getStudentGroup().getId();

        List<Long> weekTypeIds = switch (getCurrentWeekService.getCurrentWeek().toLowerCase()) {
            case "числитель" -> List.of(1L, 2L);
            case "знаменатель" -> List.of(1L, 3L);
            default -> List.of(1L);
        };

        long weekDayId = weekdaysConvertor.convertDay(currentDay);

        List<ScheduleProjection> schedule =
                scheduleRepository.findScheduleByGroupIdAndWeekdayIdAndWeekTypeIds(
                        groupId, weekDayId, weekTypeIds
                );
        System.out.println(schedule);

        Map<Integer, String> pairsFromDB = new HashMap<>();

        for (ScheduleProjection row : schedule) {
            String value = row.getSubject() + " " +
                    row.getTeacher() + " (" +
                    row.getRoom() + ")";

            pairsFromDB.put(row.getPairNumber(), value);
        }

        Map<Integer, String> sorted = new TreeMap<>(pairsFromDB);
        sorted.putAll(updateScheduleHandler.getChanges(groupName));

        StringBuilder out = new StringBuilder();
        out.append("Расписание на ")
                .append(Character.toUpperCase(currentDay.charAt(0)))
                .append(currentDay.substring(1))
                .append(" (")
                .append(getCurrentWeekService.getCurrentWeek())
                .append(")\n\n");

        for (var e : sorted.entrySet()) {
            out.append(convertToEmojiNumber(e.getKey()))
                    .append(" - ")
                    .append(e.getValue())
                    .append("\n");
        }

        return out.toString();
    }

//    public String getFullSchedule(Long userId, String weekTypeName) {
//
//        User user = userRepository.findById(userId).orElseThrow();
//
//        String groupName = user.getGroup().getName();
//
//        List<ScheduleFullProjection> schedule =
//                scheduleRepository.findScheduleByGroupNameAndWeektypeName(groupName, weekTypeName);
//
//        StringBuilder result = new StringBuilder();
//        result.append("Расписание для ").append(groupName)
//                .append(" (").append(weekTypeName).append(")\n");
//
//        String currentWeekDay = "";
//
//        for (ScheduleFullProjection el : schedule) {
//
//            if (!el.getWeekday().equals(currentWeekDay)) {
//                currentWeekDay = el.getWeekday();
//                result.append("\n").append(currentWeekDay).append("\n");
//            }
//
//            result.append(convertToEmojiNumber(el.getPairNumber()))
//                    .append(" ")
//                    .append(el.getSubject())
//                    .append(" ")
//                    .append(el.getTeacher())
//                    .append(" (")
//                    .append(el.getRoom())
//                    .append(")\n");
//        }
//
//        return result.toString();
//    }

    private static String convertToEmojiNumber(int number) {
        String[] emojiNumbers = {"0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"};
        StringBuilder emojiString = new StringBuilder();

        if (number == 0) {
            emojiString.append(emojiNumbers[0]);
        } else {
            // Преобразуем число в строку, чтобы итерировать по его цифрам в правильном порядке
            String numStr = String.valueOf(number);
            for (char c : numStr.toCharArray()) {
                // Преобразуем символ цифры в int и используем его как индекс для массива emojiNumbers
                emojiString.append(emojiNumbers[Character.getNumericValue(c)]);
            }
        }
        return emojiString.toString();
    }
}