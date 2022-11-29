package com.aztechsynergy.crickScore.model;

import lombok.*;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "points_db")
public class Points {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String matchNo;
    private String username;
    private Integer rank_no;
    private Double total;
    private Long captain;
    private Long vcaptain;
    private Long player3;
    private Long player4;
    private Long player5;
    private Long player6;
    private Long player7;
    private Long player8;
    private Long player9;
    private Double captainPoint;
    private Double vcaptainPoint;
    private Double player3Point;
    private Double player4Point;
    private Double player5Point;
    private Double player6Point;
    private Double player7Point;
    private Double player8Point;
    private Double player9Point;
}
