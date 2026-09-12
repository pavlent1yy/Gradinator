package com.pavlent1yy.gradinator.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ReferenceController {

    @PersistenceContext
    private final EntityManager em;

    @GetMapping("/teachers")
    public List<String> getTeachers() {
        return distinctFromCollections("numeratorTeachers", "denominatorTeachers");
    }

    @GetMapping("/subjects")
    public List<String> getSubjects() {
        return distinctFromCollections("numeratorSubjects", "denominatorSubjects");
    }

    @GetMapping("/rooms")
    public List<String> getRooms() {
        return distinctFromCollections("numeratorRooms", "denominatorRooms");
    }

    private List<String> distinctFromCollections(String numField, String denField) {
        Set<String> result = new TreeSet<>();
        result.addAll(em.createQuery("SELECT DISTINCT v FROM ScheduleEntry e JOIN e." + numField + " v", String.class).getResultList());
        result.addAll(em.createQuery("SELECT DISTINCT v FROM ScheduleEntry e JOIN e." + denField + " v", String.class).getResultList());
        result.remove("в предмете"); // служебное значение из веб-парсера замен, не настоящий преподаватель
        return new java.util.ArrayList<>(result);
    }
}