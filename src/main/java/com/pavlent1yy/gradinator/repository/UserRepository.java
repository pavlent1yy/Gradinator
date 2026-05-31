package com.pavlent1yy.gradinator.repository;

import com.pavlent1yy.gradinator.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("""
           SELECT u.id
           FROM User u
           WHERE u.id <> :excludedUserId
           """)
    List<Long> findAllUserIdsExcept(@Param("excludedUserId") Long excludedUserId);

    void deleteById(Long id);
}
