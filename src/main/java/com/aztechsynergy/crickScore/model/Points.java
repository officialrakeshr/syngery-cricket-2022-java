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
@Table(name = "points_db")
public class Points implements Comparable<Points>{
    @NaturalId
    @Id
    private String lookUp;
    private String matchNo;
    private String username;
    private Integer rank_no;
    private Double total;
    private Long captain;
    private Long vcaptain;
    private Long battinghero;
    private Long bowlinghero;
    private Long player5;
    private Long player6;
    private Long player7;
    private Long player8;
    private Long player9;
    private Long player10;
    private Long player11;
    private Long player12;
    private Double captainPoint;
    private Double vcaptainPoint;
    private Double battingheroPoint;
    private Double bowlingheroPoint;
    private Double player5Point;
    private Double player6Point;
    private Double player7Point;
    private Double player8Point;
    private Double player9Point;
    private Double player10Point;
    private Double player11Point;
    private Double player12Point;

    @Override
    public int compareTo(Points o) {
        return this.getTotal().compareTo(o.getTotal());
    }
}
