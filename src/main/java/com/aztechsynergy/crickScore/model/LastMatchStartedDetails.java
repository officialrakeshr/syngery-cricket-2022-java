package com.aztechsynergy.crickScore.model;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table
public class LastMatchStartedDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String matchNo;
    private Date matchStartTime;

    private Date nextMatchEnableTime;
}
