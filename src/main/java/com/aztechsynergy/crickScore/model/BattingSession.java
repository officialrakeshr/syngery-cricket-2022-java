package com.aztechsynergy.crickScore.model;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public  class BattingSession{
    public Player batterName;
    public int runs;
    public int balls;
    public int fours;
    public int sixes;
    public boolean out;
    public Player catchOrStumpedBy;
}
