package com.pavlent1yy.gradinator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    private String name;


    private String xlsxFile;
}