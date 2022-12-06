package com.aztechsynergy.crickScore.model;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "tournament")
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId
    private String matchNo;

    private String team1;

    private String team2;

    private Boolean started;

    private Boolean completed;

    private Boolean enable11;


}
