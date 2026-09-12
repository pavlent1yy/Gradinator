package com.pavlent1yy.gradinator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private LocalDate scheduleDate;


    @Column(nullable = false)
    private LocalDateTime createdAt;


    @Column(nullable = false)
    private String hash;


    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
