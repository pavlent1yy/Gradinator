package com.pavlent1yy.gradinator.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "schedule")
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int pairNumber;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String teacher;

    @Column(nullable = false)
    private String room;

    @ManyToOne
    @JoinColumn(name = "weekday_id")
    private WeekDay weekDay;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group studentGroup;

}