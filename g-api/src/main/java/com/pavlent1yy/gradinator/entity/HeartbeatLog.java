package com.pavlent1yy.gradinator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "heartbeat_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeartbeatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private LocalDateTime startedAt;


    private LocalDateTime finishedAt;


    @Enumerated(EnumType.STRING)
    private Status status;


    private String message;


    public enum Status {
        SUCCESS,
        UPDATED,
        ERROR
    }
}
