package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.WeekDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeekDayRepository extends JpaRepository<WeekDay, Long> {
    Optional<WeekDay> findWeekDayByName(String name);
}
