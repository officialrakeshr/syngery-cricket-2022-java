package com.aztechsynergy.crickScore.model;

import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "teams")
public class Team {
    @Id
    private String name;
}
