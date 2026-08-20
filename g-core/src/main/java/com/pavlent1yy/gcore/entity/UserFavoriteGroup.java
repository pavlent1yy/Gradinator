package com.pavlent1yy.gcore.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "user_favorite_groups",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_favorite_group",
                        columnNames = {"user_id", "group"}
                )
        }
)
public class UserFavoriteGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "\"group\"", nullable = false)
    private String group;
}
