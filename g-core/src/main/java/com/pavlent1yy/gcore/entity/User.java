package com.pavlent1yy.gcore.entity;

import com.pavlent1yy.gcore.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "\"User\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "group_name")
    private String group;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'STUDENT'")
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @ColumnDefault("now()")
    @Column(name = "registered_at")
    private OffsetDateTime registeredAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;
}
