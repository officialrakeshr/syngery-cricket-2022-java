package com.aztechsynergy.crickScore.model;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public  class BowlingSession{
    public Player bowlerName;
    public double overs;
    public int dots;
    public int runs;
    public int wickets;
}

