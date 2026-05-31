package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByName(String name);

    List<Group> findByNameStartingWithOrderByNameAsc(String prefix);

    @Query("""
           SELECT DISTINCT SUBSTRING(g.name, 1, 2)
           FROM Group g
           ORDER BY SUBSTRING(g.name, 1, 2)
           """)
    List<String> findDistinctPrefixes();
}
