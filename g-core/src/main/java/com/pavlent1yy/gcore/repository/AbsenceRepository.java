package com.pavlent1yy.gcore.repository;

import com.pavlent1yy.gcore.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findAllByUser_IdAndDateBetweenOrderByDateDescPairNumberAsc(Long userId, LocalDate from, LocalDate to);

    List<Absence> findAllByUser_Id(Long userId);

    List<Absence> findAllByUser_IdAndDate(Long userId, LocalDate date);

    Optional<Absence> findByUser_IdAndDateAndPairNumber(Long userId, LocalDate date, Integer pairNumber);

    void deleteAllByUser_Id(Long userId);
}
