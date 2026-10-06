package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.customExceptions.InvalidAbsenceException;
import com.pavlent1yy.gcore.dto.records.AbsenceRequest;
import com.pavlent1yy.gcore.dto.records.AbsenceResponse;
import com.pavlent1yy.gcore.dto.records.AbsenceStatsResponse;
import com.pavlent1yy.gcore.dto.records.CellData;
import com.pavlent1yy.gcore.dto.records.PairResponse;
import com.pavlent1yy.gcore.dto.records.ScheduleResponse;
import com.pavlent1yy.gcore.entity.Absence;
import com.pavlent1yy.gcore.entity.User;
import com.pavlent1yy.gcore.enums.AbsenceType;
import com.pavlent1yy.gcore.enums.WeekType;
import com.pavlent1yy.gcore.repository.AbsenceRepository;
import com.pavlent1yy.gcore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbsenceServiceTest {

    private static final String EMAIL = "user@mail.ru";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 5);

    @Mock
    private AbsenceRepository absenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private AbsenceService absenceService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setGroup("ИС1-33");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(absenceRepository.findByUser_IdAndDateAndPairNumber(any(), any(), any())).thenReturn(Optional.empty());
        when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static CellData cell(String... subjects) {
        return new CellData(List.of(subjects), List.of(), List.of());
    }

    private static Absence absence(LocalDate date, int pair, AbsenceType type) {
        Absence absence = new Absence();
        absence.setDate(date);
        absence.setPairNumber(pair);
        absence.setType(type);
        return absence;
    }

    @Test
    void marksSinglePairAsMissedWithTwoHours() {
        AbsenceResponse response = absenceService.mark(EMAIL, new AbsenceRequest(DAY, 2, AbsenceType.MISSED, " Математика "));

        assertThat(response.hours()).isEqualTo(2);
        assertThat(response.pairNumber()).isEqualTo(2);
        assertThat(response.subject()).isEqualTo("Математика");
    }

    @Test
    void lateCountsAsOneHour() {
        assertThat(absenceService.mark(EMAIL, new AbsenceRequest(DAY, 1, AbsenceType.LATE, null)).hours()).isEqualTo(1);
    }

    @Test
    void remarkingPairUpdatesExistingRecord() {
        Absence existing = absence(DAY, 3, AbsenceType.LATE);
        existing.setSubject("Физика");
        when(absenceRepository.findByUser_IdAndDateAndPairNumber(1L, DAY, 3)).thenReturn(Optional.of(existing));

        AbsenceResponse response = absenceService.mark(EMAIL, new AbsenceRequest(DAY, 3, AbsenceType.MISSED, null));

        assertThat(response.type()).isEqualTo(AbsenceType.MISSED);
        assertThat(response.subject()).isEqualTo("Физика");
        verify(absenceRepository).save(existing);
    }

    @Test
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> absenceService.mark(EMAIL, new AbsenceRequest(DAY, 0, AbsenceType.MISSED, null)))
                .isInstanceOf(InvalidAbsenceException.class);
        assertThatThrownBy(() -> absenceService.mark(EMAIL, new AbsenceRequest(DAY, 9, AbsenceType.MISSED, null)))
                .isInstanceOf(InvalidAbsenceException.class);
        assertThatThrownBy(() -> absenceService.mark(EMAIL, new AbsenceRequest(DAY, 1, null, null)))
                .isInstanceOf(InvalidAbsenceException.class);
        assertThatThrownBy(() -> absenceService.mark(EMAIL, new AbsenceRequest(LocalDate.now().plusDays(1), 1, AbsenceType.MISSED, null)))
                .isInstanceOf(InvalidAbsenceException.class);
        verify(absenceRepository, never()).save(any());
    }

    @Test
    void markDayMarksEveryPairOfWeekTypeAsMissed() {
        when(scheduleService.getSchedule("ИС1-33", DAY)).thenReturn(new ScheduleResponse(
                "ИС1-33", "Понедельник", WeekType.DENOMINATOR, DAY, List.of(
                        new PairResponse(2, cell("Физика"), cell("Химия"), false),
                        new PairResponse(1, cell("Математика"), cell(), false),
                        new PairResponse(3, cell(), cell(), false)
                )));

        List<AbsenceResponse> marked = absenceService.markDay(EMAIL, DAY);

        assertThat(marked).extracting(AbsenceResponse::pairNumber).containsExactly(1, 2);
        assertThat(marked).extracting(AbsenceResponse::subject).containsExactly("Математика", "Химия");
        assertThat(marked).allMatch(a -> a.type() == AbsenceType.MISSED && a.hours() == 2);
    }

    @Test
    void markDayOnNumeratorIgnoresDenominatorOnlyPairs() {
        when(scheduleService.getSchedule("ИС1-33", DAY)).thenReturn(new ScheduleResponse(
                "ИС1-33", "Понедельник", WeekType.NUMERATOR, DAY, List.of(
                        new PairResponse(1, cell(), cell("Химия"), false),
                        new PairResponse(2, cell("Физика"), cell(), false)
                )));

        assertThat(absenceService.markDay(EMAIL, DAY)).extracting(AbsenceResponse::pairNumber).containsExactly(2);
    }

    @Test
    void markDayFailsWithoutPairsOrGroup() {
        when(scheduleService.getSchedule("ИС1-33", DAY)).thenReturn(
                new ScheduleResponse("ИС1-33", "Воскресенье", WeekType.NUMERATOR, DAY, List.of()));

        assertThatThrownBy(() -> absenceService.markDay(EMAIL, DAY)).isInstanceOf(InvalidAbsenceException.class);

        user.setGroup(null);
        assertThatThrownBy(() -> absenceService.markDay(EMAIL, DAY)).isInstanceOf(InvalidAbsenceException.class);
    }

    @Test
    void statsAggregateByPeriods() {
        LocalDate today = LocalDate.of(2026, 10, 7);
        when(absenceRepository.findAllByUser_Id(1L)).thenReturn(List.of(
                absence(LocalDate.of(2026, 10, 6), 1, AbsenceType.MISSED),
                absence(LocalDate.of(2026, 10, 5), 2, AbsenceType.LATE),
                absence(LocalDate.of(2026, 10, 1), 1, AbsenceType.MISSED),
                absence(LocalDate.of(2026, 9, 15), 1, AbsenceType.MISSED),
                absence(LocalDate.of(2026, 5, 20), 1, AbsenceType.LATE)
        ));

        AbsenceStatsResponse stats = absenceService.stats(EMAIL, today);

        assertThat(stats.week().from()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(stats.week().hours()).isEqualTo(3);
        assertThat(stats.week().missedPairs()).isEqualTo(1);
        assertThat(stats.week().lates()).isEqualTo(1);
        assertThat(stats.month().hours()).isEqualTo(5);
        assertThat(stats.semester().from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(stats.semester().hours()).isEqualTo(7);
        assertThat(stats.total().hours()).isEqualTo(8);
        assertThat(stats.total().from()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    void semesterSplitsInSeptember() {
        assertThat(AbsenceService.semesterStart(LocalDate.of(2026, 12, 31))).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(AbsenceService.semesterStart(LocalDate.of(2027, 1, 10))).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(AbsenceService.semesterStart(LocalDate.of(2027, 8, 31))).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void rejectsInvertedOrTooLongRange() {
        assertThatThrownBy(() -> absenceService.list(EMAIL, DAY, DAY.minusDays(1)))
                .isInstanceOf(InvalidAbsenceException.class);
        assertThatThrownBy(() -> absenceService.list(EMAIL, DAY.minusYears(2), DAY))
                .isInstanceOf(InvalidAbsenceException.class);
    }
}
