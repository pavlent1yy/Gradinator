package com.pavlent1yy.gradinator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "schedule_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ScheduleSnapshot snapshot;


    @Column(nullable = false)
    private String groupName;


    @Column(nullable = false)
    private String day;


    @Column(nullable = false)
    private Integer pairNumber;


    @ElementCollection
    @CollectionTable(
            name = "entry_subjects",
            joinColumns = @JoinColumn(name = "entry_id")
    )
    @Column(name = "subject")
    private List<String> subjects;


    @ElementCollection
    @CollectionTable(
            name = "entry_teachers",
            joinColumns = @JoinColumn(name = "entry_id")
    )
    @Column(name = "teacher")
    private List<String> teachers;


    @ElementCollection
    @CollectionTable(
            name = "entry_rooms",
            joinColumns = @JoinColumn(name = "entry_id")
    )
    @Column(name = "room")
    private List<String> rooms;
}