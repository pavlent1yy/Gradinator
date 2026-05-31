package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.WeekType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeekTypeRepository extends JpaRepository<WeekType, Long> {
}
