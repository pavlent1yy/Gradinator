package com.pavlent1yy.gradinator.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import jakarta.persistence.TypedQuery;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceControllerTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<String> numeratorQuery;

    @Mock
    private TypedQuery<String> denominatorQuery;


    @InjectMocks
    private ReferenceController controller;

    @Test
    void getTeachers_shouldCombineDistinctTeachersAndRemoveServiceValue() {
        mockQueries(
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.numeratorTeachers v",
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.denominatorTeachers v",
                List.of("Ivanov", "в предмете"),
                List.of("Petrov", "Ivanov")
        );

        List<String> teachers = controller.getTeachers();

        assertThat(teachers)
                .containsExactlyInAnyOrder("Ivanov", "Petrov")
                .hasSize(2)
                .doesNotContain("в предмете");

        verify(entityManager).createQuery(
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.numeratorTeachers v",
                String.class
        );
        verify(entityManager).createQuery(
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.denominatorTeachers v",
                String.class
        );

        verify(numeratorQuery).getResultList();
        verify(denominatorQuery).getResultList();
    }

    @Test
    void getSubjects_shouldCombineDistinctSubjects() {
        mockQueries(
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.numeratorSubjects v",
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.denominatorSubjects v",
                List.of("Математика"),
                List.of("Физика")
        );

        List<String> subjects = controller.getSubjects();

        assertThat(subjects)
                .containsExactlyInAnyOrder("Математика", "Физика")
                .hasSize(2);

        verify(entityManager).createQuery(
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.numeratorSubjects v",
                String.class
        );
        verify(entityManager).createQuery(
                "SELECT DISTINCT v FROM ScheduleEntry e JOIN e.denominatorSubjects v",
                String.class
        );

        verify(numeratorQuery).getResultList();
        verify(denominatorQuery).getResultList();
    }

    private void mockQueries(
            String numeratorJpql,
            String denominatorJpql,
            List<String> numeratorResult,
            List<String> denominatorResult
    ) {
        when(entityManager.createQuery(numeratorJpql, String.class))
                .thenReturn(numeratorQuery);

        when(entityManager.createQuery(denominatorJpql, String.class))
                .thenReturn(denominatorQuery);

        when(numeratorQuery.getResultList())
                .thenReturn(numeratorResult);

        when(denominatorQuery.getResultList())
                .thenReturn(denominatorResult);
    }
}