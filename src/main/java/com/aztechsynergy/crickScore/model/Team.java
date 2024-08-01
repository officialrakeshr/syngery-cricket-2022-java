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
@Table(name = "teams")
@ToString
public class Team {
    @Id
    private String name;
    @NaturalId
    private Integer teamId;
    private String teamAbbr;
    private String logoUrl;
    private Integer squadId;
}
