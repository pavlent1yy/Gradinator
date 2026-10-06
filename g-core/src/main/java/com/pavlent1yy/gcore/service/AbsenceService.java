package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidAbsenceException;
import com.pavlent1yy.gcore.dto.records.AbsenceRequest;
import com.pavlent1yy.gcore.dto.records.AbsenceResponse;
import com.pavlent1yy.gcore.dto.records.AbsenceStatsResponse;
import com.pavlent1yy.gcore.dto.records.AbsenceStatsResponse.PeriodStats;
import com.pavlent1yy.gcore.dto.records.PairResponse;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.entity.Absence;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.AbsenceType;
import com.pavlent1yy.gcore.repository.AbsenceRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    static final int MIN_PAIR_NUMBER = 0;
    static final int MAX_PAIR_NUMBER = 6;
    private static final int MAX_RANGE_DAYS = 400;
    private static final int MAX_SUBJECT_LENGTH = 255;

    private final AbsenceRepository absenceRepository;
    private final UserRepository userRepository;
    private final ScheduleService scheduleService;

    @Transactional(readOnly = true)
    public List<AbsenceResponse> list(String email, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidAbsenceException("Неверный период");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new InvalidAbsenceException("Слишком длинный период");
        }

        return absenceRepository
                .findAllByUser_IdAndDateBetweenOrderByDateDescPairNumberAsc(getUser(email).getId(), from, to)
                .stream()
                .map(AbsenceService::toResponse)
                .toList();
    }

    @Transactional
    public AbsenceResponse mark(String email, AbsenceRequest request) {
        validateDate(request.date());
        validatePairNumber(request.pairNumber());
        if (request.type() == null) {
            throw new InvalidAbsenceException("Не указан тип отметки");
        }

        User user = getUser(email);

        return toResponse(upsert(user, request.date(), request.pairNumber(), request.type(), request.subject()));
    }

    @Transactional
    public List<AbsenceResponse> markDay(String email, LocalDate date) {
        validateDate(date);
        User user = getUser(email);

        if (user.getGroup() == null) {
            throw new InvalidAbsenceException("Сначала выбери группу в профиле");
        }

        ScheduleResponse schedule = scheduleService.getSchedule(user.getGroup(), date);
        List<PairResponse> pairs = schedule == null || schedule.pairs() == null ? List.of() : schedule.pairs();

        List<AbsenceResponse> marked = pairs.stream()
                .filter(pair -> pair.pairNumber() >= MIN_PAIR_NUMBER && pair.pairNumber() <= MAX_PAIR_NUMBER)
                .sorted(Comparator.comparingInt(PairResponse::pairNumber))
                .filter(pair -> ScheduleCells.activeCell(pair, schedule.weekType()) != null)
                .map(pair -> upsert(
                        user,
                        date,
                        pair.pairNumber(),
                        AbsenceType.MISSED,
                        String.join(", ", ScheduleCells.activeCell(pair, schedule.weekType()).subjects())
                ))
                .map(AbsenceService::toResponse)
                .toList();

        if (marked.isEmpty()) {
            throw new InvalidAbsenceException("В этот день пар нет");
        }

        return marked;
    }

    @Transactional
    public void unmark(String email, LocalDate date, Integer pairNumber) {
        absenceRepository
                .findByUser_IdAndDateAndPairNumber(getUser(email).getId(), date, pairNumber)
                .ifPresent(absenceRepository::delete);
    }

    @Transactional
    public void clearDay(String email, LocalDate date) {
        absenceRepository.deleteAll(
                absenceRepository.findAllByUser_IdAndDate(getUser(email).getId(), date)
        );
    }

    @Transactional(readOnly = true)
    public AbsenceStatsResponse stats(String email) {
        return stats(email, LocalDate.now());
    }

    AbsenceStatsResponse stats(String email, LocalDate today) {
        List<Absence> absences = absenceRepository.findAllByUser_Id(getUser(email).getId());

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate semesterStart = semesterStart(today);
        LocalDate semesterEnd = today.getMonthValue() >= 9
                ? LocalDate.of(today.getYear(), 12, 31)
                : LocalDate.of(today.getYear(), 8, 31);
        LocalDate firstAbsence = absences.stream()
                .map(Absence::getDate)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new AbsenceStatsResponse(
                periodStats(absences, weekStart, weekStart.plusDays(6)),
                periodStats(absences, monthStart, monthStart.with(TemporalAdjusters.lastDayOfMonth())),
                periodStats(absences, semesterStart, semesterEnd),
                periodStats(absences, firstAbsence, today)
        );
    }

    static LocalDate semesterStart(LocalDate date) {
        return date.getMonthValue() >= 9
                ? LocalDate.of(date.getYear(), 9, 1)
                : LocalDate.of(date.getYear(), 1, 1);
    }

    private static PeriodStats periodStats(List<Absence> absences, LocalDate from, LocalDate to) {
        List<Absence> inPeriod = absences.stream()
                .filter(a -> from == null || !a.getDate().isBefore(from))
                .filter(a -> to == null || !a.getDate().isAfter(to))
                .toList();

        return new PeriodStats(
                from,
                to,
                inPeriod.stream().mapToInt(a -> a.getType().getHours()).sum(),
                (int) inPeriod.stream().filter(a -> a.getType() == AbsenceType.MISSED).count(),
                (int) inPeriod.stream().filter(a -> a.getType() == AbsenceType.LATE).count()
        );
    }

    private Absence upsert(User user, LocalDate date, int pairNumber, AbsenceType type, String subject) {
        Absence absence = absenceRepository
                .findByUser_IdAndDateAndPairNumber(user.getId(), date, pairNumber)
                .orElseGet(() -> {
                    Absence created = new Absence();
                    created.setUser(user);
                    created.setDate(date);
                    created.setPairNumber(pairNumber);
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });

        absence.setType(type);
        String normalizedSubject = normalizeSubject(subject);
        if (normalizedSubject != null) {
            absence.setSubject(normalizedSubject);
        }

        return absenceRepository.save(absence);
    }

    private static String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        String trimmed = subject.trim();
        return trimmed.length() > MAX_SUBJECT_LENGTH ? trimmed.substring(0, MAX_SUBJECT_LENGTH) : trimmed;
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new InvalidAbsenceException("Не указана дата");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidAbsenceException("Нельзя отметить пропуск в будущем");
        }
    }

    private static void validatePairNumber(Integer pairNumber) {
        if (pairNumber == null || pairNumber < MIN_PAIR_NUMBER || pairNumber > MAX_PAIR_NUMBER) {
            throw new InvalidAbsenceException("Номер пары должен быть от " + MIN_PAIR_NUMBER + " до " + MAX_PAIR_NUMBER);
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }

    private static AbsenceResponse toResponse(Absence absence) {
        return new AbsenceResponse(
                absence.getId(),
                absence.getDate(),
                absence.getPairNumber(),
                absence.getType(),
                absence.getSubject(),
                absence.getType().getHours()
        );
    }
}
