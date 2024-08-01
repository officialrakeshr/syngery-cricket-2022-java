package com.aztechsynergy.crickScore.model;

import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "players")
public class Player {
    @Id
    private Long id;
    private String name;
    private String team;
    private Integer teamId;
    private String active;
    private transient String assignedRole;
    private String alias;
    private String imageUrl;
}