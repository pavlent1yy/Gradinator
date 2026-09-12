package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, String> {
}