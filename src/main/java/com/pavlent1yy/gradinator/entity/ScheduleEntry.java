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

    @Column(nullable = false)
    private boolean hasChanges;

    @ElementCollection @CollectionTable(name = "entry_num_subjects", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "subject")
    private List<String> numeratorSubjects;

    @ElementCollection @CollectionTable(name = "entry_num_teachers", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "teacher")
    private List<String> numeratorTeachers;

    @ElementCollection @CollectionTable(name = "entry_num_rooms", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "room")
    private List<String> numeratorRooms;

    @ElementCollection @CollectionTable(name = "entry_den_subjects", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "subject")
    private List<String> denominatorSubjects;

    @ElementCollection @CollectionTable(name = "entry_den_teachers", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "teacher")
    private List<String> denominatorTeachers;

    @ElementCollection @CollectionTable(name = "entry_den_rooms", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "room")
    private List<String> denominatorRooms;
}