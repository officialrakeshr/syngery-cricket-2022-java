package com.aztechsynergy.crickScore.model;

import lombok.*;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InningsSession {
    public String match;
    public List<BowlingSession> bowlingSession;
    public List<BattingSession> battingSession;
}

