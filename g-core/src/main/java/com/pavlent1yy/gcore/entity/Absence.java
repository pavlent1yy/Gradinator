package com.pavlent1yy.gcore.entity;

import com.pavlent1yy.gcore.enums.AbsenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "absence",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_absence_user_date_pair",
                        columnNames = {"user_id", "absence_date", "pair_number"}
                )
        }
)
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "absence_date", nullable = false)
    private LocalDate date;

    @Column(name = "pair_number", nullable = false)
    private Integer pairNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AbsenceType type;

    @Column(name = "subject")
    private String subject;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
