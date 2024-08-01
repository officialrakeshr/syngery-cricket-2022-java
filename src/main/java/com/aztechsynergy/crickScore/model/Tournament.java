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
@ToString
public class Tournament {
    @Id
    private Long id;

    @NaturalId
    private String matchNo;

    private String team1;

    private String team2;

    private Integer team1Id;

    private Integer team2Id;

    private Boolean started;

    private Boolean completed;

    private Boolean enable11;

    private String matchdate;

    private String venue;

    private String matchtime;

    private Boolean unlimitSubstitution;

    private Boolean abandoned;

    private Integer cricInfoId;

    private String cricInfoURL;


}
