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
@Table(name = "player_actions_audit")
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String matchNo;
    private String lookup;
    @Column(length = 100000)
    private String requestData;
    @Column(length = 2500)
    private String changeDetails;
    private Date timestamp;

}
