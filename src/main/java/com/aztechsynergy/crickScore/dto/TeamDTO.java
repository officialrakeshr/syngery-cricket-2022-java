package com.aztechsynergy.crickScore.dto;

import com.aztechsynergy.crickScore.model.Player;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Setter
@Getter
@Builder
public class TeamDTO{
    public ArrayList<Player> players;
    public String name;
}