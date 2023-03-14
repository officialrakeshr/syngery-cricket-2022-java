package com.aztechsynergy.crickScore.model;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.util.Date;

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
    @Builder.Default
    private Double total = 0.0;
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
    @Builder.Default
    private Double captainPoint = 0.0;
    @Builder.Default
    private Double vcaptainPoint = 0.0;
    @Builder.Default
    private Double battingheroPoint = 0.0;
    @Builder.Default
    private Double bowlingheroPoint = 0.0;
    private Double player5Point = 0.0;
    @Builder.Default
    private Double player6Point = 0.0;
    @Builder.Default
    private Double player7Point = 0.0;
    @Builder.Default
    private Double player8Point = 0.0;
    @Builder.Default
    private Double player9Point = 0.0;
    @Builder.Default
    private Double player10Point = 0.0;
    @Builder.Default
    private Double player11Point = 0.0;
    @Builder.Default
    private Double player12Point = 0.0;
    private Date lastUpdatedTime;

    private Double overSubNegativePoints;



    @Override
    public int compareTo(Points o) {
        return this.getTotal().compareTo(o.getTotal());
    }
}
