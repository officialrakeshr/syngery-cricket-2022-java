package com.aztechsynergy.crickScore.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class PointFE {
    private Double total = 0.0;
    private String lookUp;
    private String matchNo;
    private String captain;
    private String vcaptain;
    private String battinghero;
    private String bowlinghero;
    private String player5;
    private String player6;
    private String player7;
    private String player8;
    private String player9;
    private String player10;
    private String player11;
    private String player12;
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

    @Builder.Default
    private Double overSubNegativePoints = 0.0;
}
