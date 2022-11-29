package com.aztechsynergy.crickScore.model;

import lombok.*;

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

    private String matchNo;

    private String team1;

    private String team2;

    private Boolean completed;

    private Boolean enable11;


}
