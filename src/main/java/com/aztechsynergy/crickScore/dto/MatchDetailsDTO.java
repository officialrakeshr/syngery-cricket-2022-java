package com.aztechsynergy.crickScore.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class MatchDetailsDTO {
    public TeamDTO team1;
    public TeamDTO team2;
    public String matchNo;
}


