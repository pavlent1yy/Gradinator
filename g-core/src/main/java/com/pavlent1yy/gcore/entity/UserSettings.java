package com.pavlent1yy.gcore.entity;

import com.pavlent1yy.gcore.enums.Theme;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 20)
    private Theme theme;

    @Column(name = "current_group")
    private String currentGroup;

    @OneToMany(
            mappedBy = "userSettings",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<UserFavoriteGroup> favoriteGroups;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    @Column(name = "show_changes", nullable = false)
    private boolean showChanges;
}
