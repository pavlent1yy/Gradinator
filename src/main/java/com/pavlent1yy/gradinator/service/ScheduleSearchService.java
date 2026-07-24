package com.pavlent1yy.gradinator.service;

import com.pavlent1yy.gradinator.entity.ScheduleEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ScheduleSearchService {

    @PersistenceContext
    private final EntityManager em;

    public List<ScheduleEntry> search(String teacher, String group, String room, String subject, LocalDate date, Integer pair) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ScheduleEntry> cq = cb.createQuery(ScheduleEntry.class);
        Root<ScheduleEntry> root = cq.from(ScheduleEntry.class);
        cq.distinct(true);

        List<Predicate> predicates = new ArrayList<>();

        if (group != null) predicates.add(cb.equal(root.get("groupName"), group));
        if (pair != null) predicates.add(cb.equal(root.get("pairNumber"), pair));

        if (date != null) {
            Join<Object, Object> snapshotJoin = root.join("snapshot");
            predicates.add(cb.equal(snapshotJoin.get("scheduleDate"), date));
        }

        if (teacher != null) {
            Join<Object, String> num = root.join("numeratorTeachers", JoinType.LEFT);
            Join<Object, String> den = root.join("denominatorTeachers", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.like(num, "%" + teacher + "%"),
                    cb.like(den, "%" + teacher + "%")
            ));
        }

        if (subject != null) {
            Join<Object, String> num = root.join("numeratorSubjects", JoinType.LEFT);
            Join<Object, String> den = root.join("denominatorSubjects", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.like(num, "%" + subject + "%"),
                    cb.like(den, "%" + subject + "%")
            ));
        }

        if (room != null) {
            Join<Object, String> num = root.join("numeratorRooms", JoinType.LEFT);
            Join<Object, String> den = root.join("denominatorRooms", JoinType.LEFT);
            predicates.add(cb.or(
                    cb.like(num, "%" + room + "%"),
                    cb.like(den, "%" + room + "%")
            ));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        return em.createQuery(cq).getResultList();
    }
}
