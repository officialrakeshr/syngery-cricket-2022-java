package com.aztechsynergy.crickScore.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@ToString
@Table(name = "score_db")
public class ScoringDB {
    @Id
    private String lookup;
    private String matchNo;
    private Long playerId;
    private String playerName;
    private Boolean batting;
    private Boolean bowling;
    private Integer runs;
    private Integer ballsFaced;
    private Integer fours;
    private Integer sixes;
    private Boolean duck;
    private Double  overs;
    private Integer  dots;
    private Integer  runsConceded;
    private Integer  wickets;
    private Integer  catchOrStumps;
    private Integer  score30;
    private Integer  score50;
    private Integer  score100;
    private Integer  score150;
    private Integer  wicket3;
    private Integer  wicket4;
    private Integer  wicket5;
    private Integer  wicket6;
    private Integer  runScoreBonus;
    private Integer  foursAndSixesBonus;
    private Integer  bonusFor30_50_100_150;
    private Integer  duckBonus;
    private Integer  wicketBonus;
    private Integer  dotBonus;
    private Integer  bonusFor3wk4wk5wk6wk;
    private Integer  catchBonus;
    private Double strikeRate;
    private Double economy;
    private Integer  sRBonus;
    private Integer  economyBonus;
    private Integer battingPoints;
    private Integer bowlingPoints;
    private Integer battingHeroBonus;
    private Integer bowlingHeroBonus;
    private Integer  totalPlayerPoint;
}
