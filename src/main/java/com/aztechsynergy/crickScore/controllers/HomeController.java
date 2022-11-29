package com.aztechsynergy.crickScore.controllers;

import com.aztechsynergy.crickScore.dto.MatchDetailsDTO;
import com.aztechsynergy.crickScore.model.Player;
import com.aztechsynergy.crickScore.model.Team;
import com.aztechsynergy.crickScore.model.Tournament;
import com.aztechsynergy.crickScore.repository.*;
import com.aztechsynergy.crickScore.security.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping(path="/cricket")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class HomeController {

    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder encoder;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TournamentRepository tournamentRepository;

    @GetMapping("/players")
    public ResponseEntity<?> listPlayers(@RequestParam(required = false) String team) {
        Map<String, List<Player>> players = new HashMap<>();
        if(StringUtil.isNotBlank(team)){
            players.put("players",playerRepository.findByTeam(team));
        }
        else players.put("players",playerRepository.findAll());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/teams")
    public ResponseEntity<?> listTeams() {
        Map<String, List<Team>> players = new HashMap<>();
        players.put("teams",teamRepository.findAll());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/tournaments")
    public ResponseEntity<?> listTournament() {
        Map<String, List<Tournament>> players = new HashMap<>();
        players.put("tournaments",tournamentRepository.findAll());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/matchDetails")
    public ResponseEntity<?> matchDetails(@RequestParam String matchNo) {
        Map<String, Object> match = new HashMap<>();
        Tournament a = tournamentRepository.findDistinctFirstByMatchNo(matchNo);
        match.put("matchNo",matchNo);
        HashMap<String, Object> team1 = new HashMap<>();
        team1.put("name",a.getTeam1());
        team1.put("players",playerRepository.findByTeam(a.getTeam1()));
        match.put("team1",team1);

        HashMap<String, Object> team2 = new HashMap<>();
        team2.put("name",a.getTeam2());
        team2.put("players",playerRepository.findByTeam(a.getTeam2()));
        match.put("team2",team2);

        return ResponseEntity.ok(match);
    }

    @PutMapping("/updateMatchDetails")
    public ResponseEntity<?> updateMatchDetails(@RequestBody MatchDetailsDTO match) {

        playerRepository.saveAll(match.getTeam1().getPlayers());
        playerRepository.saveAll(match.getTeam2().getPlayers());

        return ResponseEntity.ok(true);
    }

    @PutMapping("/updateTournament")
    public ResponseEntity<?> updateTournament(@RequestBody Tournament match) {

        tournamentRepository.save(match);

        return ResponseEntity.ok(tournamentRepository.save(match));
    }


}
