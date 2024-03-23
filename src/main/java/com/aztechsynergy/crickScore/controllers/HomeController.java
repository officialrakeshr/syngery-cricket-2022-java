package com.aztechsynergy.crickScore.controllers;

import com.aztechsynergy.crickScore.dto.MatchDetailsDTO;
import com.aztechsynergy.crickScore.dto.TeamDTO;
import com.aztechsynergy.crickScore.model.*;
import com.aztechsynergy.crickScore.repository.*;
import com.aztechsynergy.crickScore.security.jwt.JwtProvider;
import com.aztechsynergy.crickScore.services.CricInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/cricket")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 86400)
public class HomeController {

    //constructor of the DecimalFormat class
    private static final DecimalFormat decfor = new DecimalFormat("0.00");

    @Value("${cricket.cricinfo.seriesId}")
    private  Integer SeriesID;
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

    @Autowired
    ScoringDbRepository scoringDbRepository;

    @Autowired
    PointsRepository pointsRepository;

    @Autowired
    SubstitutionRepository substitutionRepository;

    @Autowired
    LastMatchStartedRepository lastMatchStartedRepository;

    @Autowired
    CricInfoService cricInfoService;

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private AuditRepository auditRepository;

    @GetMapping("/players")
    public ResponseEntity<?> listPlayers(
            @RequestParam(required = false) String team
    ) {
        Map<String, List<Player>> players = new HashMap<>();
        if (StringUtil.isNotBlank(team)) {
            players.put("players", playerRepository.findByTeam(team));
        } else players.put("players", playerRepository.findAll());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/teams")
    public ResponseEntity<?> listTeams() {
        Map<String, List<Team>> players = new HashMap<>();
        players.put("teams", teamRepository.findAll());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/tournaments")
    public ResponseEntity<?> listTournament() {
        Map<String, List<Tournament>> players = new HashMap<>();
        players.put("tournaments", tournamentRepository.findAll());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/tournaments/{matchNo}")
    public ResponseEntity<?> getMatchDetailsByMatchNo(
            @PathVariable String matchNo
    ) {
        return ResponseEntity.ok(
                tournamentRepository.findDistinctFirstByMatchNo(matchNo)
        );
    }

    @GetMapping("/getPlayerPointsByMatch/{matchNo}")
    public ResponseEntity<?> getPlayerPointsByMatch(
            @PathVariable String matchNo
    ) {
        return ResponseEntity.ok(
                tournamentRepository.findPlayerPointsByMatch(matchNo)
        );
    }

    @PostMapping("/createPlayer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPlayer(@RequestBody Player player) {
        boolean found = playerRepository
                .findByTeam(player.getTeam())
                .stream()
                .anyMatch(o -> o.getName().equals(player.getName()));
        if (found) return ResponseEntity.ok(null);
        return ResponseEntity.ok(
                playerRepository.save(
                        Player
                                .builder()
                                .active("active")
                                .team(player.getTeam())
                                .name(player.getName())
                                .alias(player.getAlias())
                                .build()
                )
        );
    }

    @GetMapping("/matchDetails")
    public ResponseEntity<?> matchDetails(@RequestParam String matchNo) {
        Tournament a = tournamentRepository.findDistinctFirstByMatchNo(matchNo);
        return ResponseEntity.ok(
                MatchDetailsDTO
                        .builder()
                        .matchNo(matchNo)
                        .team1(
                                TeamDTO
                                        .builder()
                                        .name(a.getTeam1())
                                        .players(playerRepository.findByTeam(a.getTeam1()))
                                        .build()
                        )
                        .team2(
                                TeamDTO
                                        .builder()
                                        .name(a.getTeam2())
                                        .players(playerRepository.findByTeam(a.getTeam2()))
                                        .build()
                        )
                        .build()
        );
    }

    @PutMapping("/updateMatchDetails")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMatchDetails(
            @RequestBody MatchDetailsDTO match
    ) {
        playerRepository.saveAll(match.getTeam1().getPlayers());
        playerRepository.saveAll(match.getTeam2().getPlayers());

        return ResponseEntity.ok(true);
    }

    @PutMapping("/abandonMatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> abandonMatch(@RequestBody Tournament match) {
        String matchNo = match.getMatchNo();
        userRepository
                .findAllUserIds()
                .forEach(o -> {
                    Optional<Substitution> prevMatchSub = substitutionRepository.findById(
                            pointsLookup(String.valueOf(Integer.parseInt(matchNo) - 1), o)
                    );
                    if (prevMatchSub.isPresent()) {
                        Substitution preSubDa = prevMatchSub.get();
                        int prevSubUnused = preSubDa.getTotal() - preSubDa.getUsed() > -1
                                ? preSubDa.getTotal() - preSubDa.getUsed()
                                : 0;
                        substitutionRepository.save(
                                Substitution
                                        .builder()
                                        .lookUp(pointsLookup(matchNo, o))
                                        .username(o)
                                        .matchNo(matchNo)
                                        .free(3)
                                        .total((3 + prevSubUnused) > 5 ? 3 : 3 + prevSubUnused)
                                        .used(0)
                                        .build()
                        );
                    }
                    Points lastPoint = pointsRepository.findByLookUp(
                            pointsLookup(String.valueOf(Integer.parseInt(matchNo) - 1), o)
                    );
                    if (lastPoint != null) {
                        pointsRepository.save(
                                Points
                                        .builder()
                                        .lookUp(pointsLookup(matchNo, o))
                                        .matchNo(matchNo)
                                        .username(o)
                                        .captain(lastPoint.getCaptain())
                                        .vcaptain(lastPoint.getVcaptain())
                                        .battinghero(lastPoint.getBattinghero())
                                        .bowlinghero(lastPoint.getBowlinghero())
                                        .player5(lastPoint.getPlayer5())
                                        .player6(lastPoint.getPlayer6())
                                        .player7(lastPoint.getPlayer7())
                                        .player8(lastPoint.getPlayer8())
                                        .player9(lastPoint.getPlayer9())
                                        .player10(lastPoint.getPlayer10())
                                        .player11(lastPoint.getPlayer11())
                                        .player12(lastPoint.getPlayer12())
                                        .lastUpdatedTime(new Date())
                                        .build()
                        );
                    }
                });
        match.setAbandoned(true);
        return ResponseEntity.ok(tournamentRepository.save(match));
    }

    @PutMapping("/updateTournament")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTournament(@RequestBody Tournament match) {
        if (match.getEnable11()) {
            List<Tournament> old = tournamentRepository
                    .findAll()
                    .stream()
                    .peek(o -> o.setEnable11(false))
                    .collect(Collectors.toList());
            tournamentRepository.saveAll(old);

            //from points
            List<String> totalPlayers = userRepository.findAllUserIds();
            List<String> matchUsers = pointsRepository.findUsersByMatchNo(
                    match.getMatchNo()
            );
            totalPlayers.removeAll(matchUsers);
            totalPlayers.forEach(p -> {
                Optional<Points> lastPoint = pointsRepository
                        .findPointsInDescLastUpdatedPoints(p)
                        .stream()
                        .findFirst();
                if (lastPoint.isPresent()) {
                    Points last = lastPoint.get();
                    pointsRepository.save(
                            Points
                                    .builder()
                                    .lookUp(pointsLookup(match.getMatchNo(), p))
                                    .matchNo(match.getMatchNo())
                                    .username(last.getUsername())
                                    .captain(last.getCaptain())
                                    .vcaptain(last.getVcaptain())
                                    .battinghero(last.getBattinghero())
                                    .bowlinghero(last.getBowlinghero())
                                    .player5(last.getPlayer5())
                                    .player6(last.getPlayer6())
                                    .player7(last.getPlayer7())
                                    .player8(last.getPlayer8())
                                    .player9(last.getPlayer9())
                                    .player10(last.getPlayer10())
                                    .player11(last.getPlayer11())
                                    .player12(last.getPlayer12())
                                    .lastUpdatedTime(new Date())
                                    .build()
                    );
                }

                Optional<Substitution> prevMatchSub = substitutionRepository.findById(
                        pointsLookup(
                                String.valueOf(Integer.parseInt(match.getMatchNo()) - 1),
                                p
                        )
                );
                if (prevMatchSub.isPresent()) {
                    Substitution prevSub = prevMatchSub.get();
                    int prevSubUnused = prevSub.getTotal() - prevSub.getUsed() > -1
                            ? prevSub.getTotal() - prevSub.getUsed()
                            : 0;
                    Optional<Substitution> subMatch = substitutionRepository.findById(
                            pointsLookup(match.getMatchNo(), p)
                    );
                    if (!subMatch.isPresent()) {
                        substitutionRepository.save(
                                Substitution
                                        .builder()
                                        .lookUp(pointsLookup(match.getMatchNo(), p))
                                        .username(p)
                                        .matchNo(match.getMatchNo())
                                        .free(3)
                                        .total((3 + prevSubUnused) > 5 ? 3 : 3 + prevSubUnused)
                                        .used(0)
                                        .build()
                        );
                    }
                }
            });
        }
        /**if(match.getStarted()){
         List<Tournament> old = tournamentRepository.findAll().stream().peek(o -> o.setStarted(false)).collect(Collectors.toList());
         tournamentRepository.saveAll(old);
         }**/
        tournamentRepository.save(match);

        return ResponseEntity.ok(tournamentRepository.save(match));
    }

    @GetMapping("/getActiveDream9Tournament")
    public ResponseEntity<?> getActiveDream9Tournament() {
        Optional<Tournament> data = tournamentRepository
                .findAll()
                .stream()
                .filter(Tournament::getEnable11)
                .findFirst();

        return ResponseEntity.ok(data.orElse(null));
    }

    @GetMapping("/getAllActiveDream9Tournaments")
    public ResponseEntity<?> getAllActiveDream9Tournaments() {
        List<Tournament> data = tournamentRepository
                .findAll()
                .stream()
                .filter(Tournament::getEnable11)
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/getCricInfoMatchDetails")
    public ResponseEntity<?> getCricInfoMatchDetails(
            @RequestParam String  matchNo
    ) {
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(
                matchNo
        );
        return ResponseEntity.ok(
                cricInfoService.getIPLMatchScoreCard("en", SeriesID, tournament.getCricInfoId())
        );
    }

    @GetMapping("/getScoreCardByMatchNo")
    public ResponseEntity<?> getScoreCardByMatchNo(
            @RequestParam String  matchNo
    ) {
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(
                matchNo
        );
        Map<String, Object> match = cricInfoService.getIPLMatchDetails("en", SeriesID, tournament.getCricInfoId(), true);
        return ResponseEntity.ok(
                match.get("scorecard")
        );
    }

    @GetMapping("/getOverDetailsByMatchNo")
    public ResponseEntity<?> getOverDetailsByMatchNo(
            @RequestParam String  matchNo
    ) {
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(
                matchNo
        );
        Map<String, Object> match = cricInfoService.getIPLMatchOverDetails("en", SeriesID, tournament.getCricInfoId(), "ALL");
        return ResponseEntity.ok(
                match
        );
    }

    @GetMapping("/isMatchFirstBallThrown")
    public ResponseEntity<Boolean> isMatchFirstBallThrown(
            @RequestParam String matchNo
    ) {
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(
                matchNo
        );
        if (tournament != null) {
            Map<String, Object> match = (Map<String, Object>) cricInfoService
                    .getIPLMatchScoreCard("en", SeriesID, tournament.getCricInfoId())
                    .get("match");
            return ResponseEntity.ok(
                    match.get("liveOvers") != null && (Double) match.get("liveOvers") > 0.00
            );
        }
        return ResponseEntity.ok(false);
    }

    @Scheduled(cron = "0/20 * 15-23 * * ?", zone = "Asia/Kolkata")
    public void scheduledMatchStartCheck() {
        LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String pattern = "MMMM d, yyyy";
        String date = currentDate.format(DateTimeFormatter.ofPattern(pattern));
        System.out.println("In scheduledMatchStartCheck ::"+date+":::Current server Time:"+ new Date());
        LastMatchStartedDetails LstStartedmatch = lastMatchStartedRepository.findAll().stream().findFirst().orElse(null);
        if(LstStartedmatch !=null && LstStartedmatch.getNextMatchEnableTime().before(new Date())){
            int i = Integer.parseInt(LstStartedmatch.getMatchNo()) + 1;
            Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(String.valueOf(i));
            if(tournament!=null){
                tournament.setEnable11(true);
                this.updateTournament(tournament);
                lastMatchStartedRepository.deleteAll();
                this.template.convertAndSend("/topic/pushMessage", "\"Fantastic 12\" enabled for the match no:"+i);
                this.template.convertAndSend("/topic/reloadPage", true);
            }
        }
        List<Tournament> enabledMatches = tournamentRepository.findTournamentsByEnable11AndMatchdate(
                date
        );

        enabledMatches.forEach(p -> {
            Map<String, Object> match = (Map<String, Object>) cricInfoService
                    .getIPLMatchScoreCard("en", 1345038, p.getCricInfoId())
                    .get("match");
            if (
                    match.get("liveOvers") != null && (Double) match.get("liveOvers") > 0.00
            ) {
                System.out.println("Match" + p.getMatchNo() + " first ball completed");
                p.setStarted(true);
                p.setEnable11(false);
                tournamentRepository.save(p);
                lastMatchStartedRepository.save(LastMatchStartedDetails.builder().matchNo(p.getMatchNo()).matchStartTime(new Date()).nextMatchEnableTime(addMinutesToDate(5, new Date())).build());
                this.template.convertAndSend("/topic/pushMessage", "Match" + p.getMatchNo() + " first ball completed");
                this.template.convertAndSend("/topic/reloadPage", true);
            }
        });
    }
    private Date addMinutesToDate(int minutes, Date beforeTime) {

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs
                + (minutes * 60000));
        return afterAddingMins;
    }
    @GetMapping("/attemptHack")
    public ResponseEntity<?> attemptHack() {
        return ResponseEntity.ok(true);
    }

    @GetMapping("/getStartedTournament")
    public ResponseEntity<?> getStartedTournament() {
        Optional<Tournament> data = tournamentRepository
                .findAll()
                .stream()
                .filter(Tournament::getStarted)
                .findFirst();

        return ResponseEntity.ok(data.orElse(null));
    }

    @GetMapping("/getStartedAllTournament")
    public ResponseEntity<?> getStartedAllTournament() {
        List<Tournament> data = tournamentRepository
                .findAll()
                .stream()
                .filter(Tournament::getStarted)
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/scoreSplitForPlayers")
    public ResponseEntity<?> scoreSplitForPlayers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (user.isPresent()) {
            List<Points> points = pointsRepository.findPointsInDescLastUpdatedPoints(
                    user.get().getUsername()
            );
            Map<Long, String> playerMap = playerRepository
                    .listPlayerNames()
                    .stream()
                    .collect(Collectors.toMap(Player::getId, Player::getName));
            List<PointFE> customised = new ArrayList<>();
            for (Points point : points) {
                customised.add(
                        PointFE
                                .builder()
                                .matchNo(point.getMatchNo())
                                .lookUp(point.getLookUp())
                                .battinghero(playerMap.get(point.getBattinghero()))
                                .battingheroPoint(point.getBattingheroPoint())
                                .bowlinghero(playerMap.get(point.getBowlinghero()))
                                .bowlingheroPoint(point.getBowlingheroPoint())
                                .captain(playerMap.get(point.getCaptain()))
                                .captainPoint(point.getCaptainPoint())
                                .vcaptain(playerMap.get(point.getVcaptain()))
                                .vcaptainPoint(point.getVcaptainPoint())
                                .player5(playerMap.get(point.getPlayer5()))
                                .player5Point(point.getPlayer5Point())
                                .player6(playerMap.get(point.getPlayer6()))
                                .player6Point(point.getPlayer6Point())
                                .player7(playerMap.get(point.getPlayer7()))
                                .player7Point(point.getPlayer7Point())
                                .player8(playerMap.get(point.getPlayer8()))
                                .player8Point(point.getPlayer8Point())
                                .player9(playerMap.get(point.getPlayer9()))
                                .player9Point(point.getPlayer9Point())
                                .player10(playerMap.get(point.getPlayer10()))
                                .player10Point(point.getPlayer10Point())
                                .player11(playerMap.get(point.getPlayer11()))
                                .player11Point(point.getPlayer11Point())
                                .player12(playerMap.get(point.getPlayer12()))
                                .player12Point(point.getPlayer12Point())
                                .overSubNegativePoints(point.getOverSubNegativePoints())
                                .total(point.getTotal())
                                .build()
                );
            }
            return ResponseEntity.ok(customised);
        }
        return null;
    }

    @GetMapping("/resetToPreviousDay/{matchNo}")
    public ResponseEntity<?> resetToPreviousDay(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear,
            @PathVariable String matchNo
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (user.isPresent()) {
            Optional<Substitution> prevMatchSub = substitutionRepository.findById(
                    pointsLookup(
                            String.valueOf(Integer.parseInt(matchNo) - 1),
                            user.get().getUsername()
                    )
            );
            if (prevMatchSub.isPresent()) {
                Substitution preSubDa = prevMatchSub.get();
                int prevSubUnused = preSubDa.getTotal() - preSubDa.getUsed() > -1
                        ? preSubDa.getTotal() - preSubDa.getUsed()
                        : 0;
                substitutionRepository.save(
                        Substitution
                                .builder()
                                .lookUp(pointsLookup(matchNo, user.get().getUsername()))
                                .username(user.get().getUsername())
                                .matchNo(matchNo)
                                .free(3)
                                .total((3 + prevSubUnused) > 5 ? 3 : 3 + prevSubUnused)
                                .used(0)
                                .build()
                );
            }
            Points lastPoint = pointsRepository.findByLookUp(
                    pointsLookup(
                            String.valueOf(Integer.parseInt(matchNo) - 1),
                            user.get().getUsername()
                    )
            );
            if (lastPoint != null) {
                pointsRepository.save(
                        Points
                                .builder()
                                .lookUp(pointsLookup(matchNo, user.get().getUsername()))
                                .matchNo(matchNo)
                                .username(user.get().getUsername())
                                .captain(lastPoint.getCaptain())
                                .vcaptain(lastPoint.getVcaptain())
                                .battinghero(lastPoint.getBattinghero())
                                .bowlinghero(lastPoint.getBowlinghero())
                                .player5(lastPoint.getPlayer5())
                                .player6(lastPoint.getPlayer6())
                                .player7(lastPoint.getPlayer7())
                                .player8(lastPoint.getPlayer8())
                                .player9(lastPoint.getPlayer9())
                                .player10(lastPoint.getPlayer10())
                                .player11(lastPoint.getPlayer11())
                                .player12(lastPoint.getPlayer12())
                                .lastUpdatedTime(new Date())
                                .build()
                );
            }
            LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            String pattern = "yyyy-MM-dd HH:mm:ss";
            String date = localDateTime.format(DateTimeFormatter.ofPattern(pattern));
            auditRepository.save(Audit.builder().matchNo(matchNo).lookup(pointsLookup(
                    String.valueOf(Integer.parseInt(matchNo)),
                    user.get().getUsername()
            )).userId(user.get().getUsername()).changeDetails("ResetToPreviousDay::"+date).timestamp(new Date()).build());
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.ok(false);
    }

    @PostMapping("/updateDream9Details")
    public ResponseEntity<?> updateDream9Details(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear,
            @RequestBody Points points
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (!user.isPresent()) return ResponseEntity.ok(false);
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(
                points.getMatchNo()
        );
        user.ifPresent(value -> points.setUsername(value.getUsername()));
        if (
                null == tournament ||
                        (tournament.getStarted() || !tournament.getEnable11())
        ) return ResponseEntity.ok(false);
        points.setLookUp(pointsLookup(points.getMatchNo(), points.getUsername()));
        points.setLastUpdatedTime(new Date());
        ///AUDIT /////
        auditRepository.save(Audit.builder().matchNo(points.getMatchNo()).lookup(pointsLookup(
                String.valueOf(Integer.parseInt(points.getMatchNo())),
                user.get().getUsername()
        )).userId(user.get().getUsername()).changeDetails("updateDream9Details(Role Change or Team setting) :"+calculateChanges(points)).requestData(points.toString()).timestamp(new Date()).build());
        /////////////////////
        pointsRepository.save(points);
        String infinteSubAvailable = substitutionRepository.findMatchByInfinitSubs(
                points.getUsername()
        );
        if (!StringUtil.isNotBlank(infinteSubAvailable)) {
            substitutionRepository.save(
                    Substitution
                            .builder()
                            .lookUp(pointsLookup(points.getMatchNo(), points.getUsername()))
                            .username(points.getUsername())
                            .matchNo(points.getMatchNo())
                            .free(100000)
                            .total(100000)
                            .used(0)
                            .build()
            );
        }
        return ResponseEntity.ok(true);
    }

    @PostMapping("/updateSubstitutionsAndConfig")
    public ResponseEntity<?> updateSubstitutions(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear,
            @RequestBody Points points
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (!user.isPresent()) return ResponseEntity.ok(false);
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(
                points.getMatchNo()
        );
        user.ifPresent(value -> points.setUsername(value.getUsername()));
        if (
                null == tournament ||
                        (tournament.getStarted() || !tournament.getEnable11())
        ) return ResponseEntity.ok(false);
        points.setLookUp(pointsLookup(points.getMatchNo(), points.getUsername()));
        points.setLastUpdatedTime(new Date());
        Optional<Substitution> prevMatchSub = substitutionRepository.findById(
                pointsLookup(
                        String.valueOf(Integer.parseInt(points.getMatchNo()) - 1),
                        points.getUsername()
                )
        );
        if (prevMatchSub.isPresent()) {
            Substitution prevSub = prevMatchSub.get();
            int prevSubUnused = prevSub.getTotal() - prevSub.getUsed() > -1
                    ? prevSub.getTotal() - prevSub.getUsed()
                    : 0;
            Optional<Substitution> subMatch = substitutionRepository.findById(
                    pointsLookup(points.getMatchNo(), points.getUsername())
            );
            if (subMatch.isPresent()) {
                Substitution subV = subMatch.get();
                subV.setFree(3);
                subV.setTotal((3 + prevSubUnused) > 5 ? 3 : 3 + prevSubUnused);
                subV.setUsed(subV.getUsed() + 1);
            } else {
                substitutionRepository.save(
                        Substitution
                                .builder()
                                .lookUp(pointsLookup(points.getMatchNo(), points.getUsername()))
                                .username(points.getUsername())
                                .matchNo(points.getMatchNo())
                                .free(3)
                                .total((3 + prevSubUnused) > 5 ? 3 : 3 + prevSubUnused)
                                .used(1)
                                .build()
                );
            }
        } else {
            Optional<Substitution> sub = substitutionRepository.findById(
                    pointsLookup(points.getMatchNo(), points.getUsername())
            );
            String infiniteSubMatch = substitutionRepository.findMatchByInfinitSubs(
                    points.getUsername()
            );
            if (!sub.isPresent()) {
                substitutionRepository.save(
                        Substitution
                                .builder()
                                .lookUp(pointsLookup(points.getMatchNo(), points.getUsername()))
                                .username(points.getUsername())
                                .matchNo(points.getMatchNo())
                                .free(!StringUtil.isNotBlank(infiniteSubMatch) ? 100000 : 2)
                                .total(!StringUtil.isNotBlank(infiniteSubMatch) ? 100000 : 2)
                                .used(1)
                                .build()
                );
            } else {
                Substitution sub1 = sub.get();
                sub1.setUsed(sub1.getUsed() + 1);
                substitutionRepository.save(sub1);
            }
        }
        //Audit/////////////
        auditRepository.save(Audit.builder().matchNo(points.getMatchNo()).lookup(pointsLookup(
                String.valueOf(Integer.parseInt(points.getMatchNo())),
                user.get().getUsername()
        )).userId(user.get().getUsername()).changeDetails("updateSubstitutionsAndConfig : "+calculateChanges(points)).requestData(points.toString()).timestamp(new Date()).build());
        /////////////////////////////////
        pointsRepository.save(points);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/getSubstitutionStatus/{matchNo}")
    public ResponseEntity<?> getSubstitutionStatus(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear,
            @PathVariable String matchNo
    ) {
        Optional<User> user = findGuestByToken(bear);
        return user
                .map(value ->
                        ResponseEntity.ok(
                                substitutionRepository
                                        .findById(pointsLookup(matchNo, value.getUsername()))
                                        .orElse(Substitution.builder().free(100000).total(100000).build())
                        )
                )
                .orElseGet(() -> ResponseEntity.ok(Substitution.builder().build()));
    }

    @GetMapping("/getDream9playerConfig/{matchNo}")
    public ResponseEntity<?> getDream9playerConfig(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear,
            @PathVariable String matchNo
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (user.isPresent()) {
            Tournament t = tournamentRepository.findDistinctFirstByMatchNo(matchNo);
            if (!t.getEnable11()) return ResponseEntity.ok(null);
            List<Player> playerList = new ArrayList<>();
            Points points = pointsRepository.findByLookUp(
                    pointsLookup(matchNo, user.get().getUsername())
            );
            if (points != null) {
                Map<Long, String> map = new HashMap<>();
                map.put(points.getCaptain(), "captain");
                map.put(points.getVcaptain(), "vcaptain");
                map.put(points.getBowlinghero(), "bowlinghero");
                map.put(points.getBattinghero(), "battinghero");
                map.put(points.getPlayer5(), "player5");
                map.put(points.getPlayer6(), "player6");
                map.put(points.getPlayer7(), "player7");
                map.put(points.getPlayer8(), "player8");
                map.put(points.getPlayer9(), "player9");
                map.put(points.getPlayer10(), "player10");
                map.put(points.getPlayer11(), "player11");
                map.put(points.getPlayer12(), "player12");

                List<Long> listOfPlayerIds = new ArrayList<>(map.keySet());
                List<Player> players = playerRepository.findByIdIsIn(listOfPlayerIds);

                players.forEach(player -> {
                    player.setAssignedRole(map.get(player.getId()));
                    playerList.add(player);
                });

                return ResponseEntity.ok(playerList);
            }
        } else {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(null);
    }

    @GetMapping("/getDream9Details")
    public ResponseEntity<?> getDream9Details(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (!user.isPresent()) return ResponseEntity.ok(false);
        return ResponseEntity.ok(
                pointsRepository.findByUsername(user.get().getUsername())
        );
    }

    @GetMapping("/findAllRankForPlayers")
    public ResponseEntity<?> findAllRankForPlayers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (!user.isPresent()) return ResponseEntity.ok(false);
        return ResponseEntity.ok(
                pointsRepository.findAllRankForPlayers(user.get().getMatchNumber())
        );
    }

    @GetMapping("/findAllRankByMatch")
    public ResponseEntity<?> findAllRankByMatch(
            @RequestParam(name = "matchNo") String matchNo
    ) {
        return ResponseEntity.ok(pointsRepository.findAllRankForPlayers(matchNo));
    }

    @GetMapping("/findLeagueRankForPlayers")
    public ResponseEntity<?> findLeagueRankForPlayers(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bear
    ) {
        Optional<User> user = findGuestByToken(bear);
        if (!user.isPresent()) return ResponseEntity.ok(false);
        return ResponseEntity.ok(pointsRepository.findLeagueRankForPlayers());
    }

    @PostMapping("/createNewMatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createNewMatch(@RequestBody Tournament tournament) {
        Tournament res = tournamentRepository.save(tournament);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/updateInningsSession")
    public ResponseEntity<?> updateInningsSession(
            @RequestBody InningsSession match
    ) {
        System.out.println(match.toString());
        List<BattingSession> battingSession = match.getBattingSession();
        List<BowlingSession> bowlingSession = match.getBowlingSession();
        Map<String, Integer> catchStump = new HashMap<>();
        battingSession.forEach(o -> {
            if (Objects.nonNull(o.getCatchOrStumpedBy())) {
                Integer temp = catchStump.get(
                        lookUpMaker(match, o.getCatchOrStumpedBy())
                );
                catchStump.put(
                        lookUpMaker(match, o.getCatchOrStumpedBy()),
                        temp != null ? temp + 1 : 1
                );
            }
        });
        battingSession.forEach(o -> {
            Player batter = o.getBatterName();
            Player catchOrStumpBy = o.getCatchOrStumpedBy();
            ScoringDB batterData = scoringDbRepository.findScoringDBByLookup(
                    lookUpMaker(match, batter)
            );
            ScoringDB catchOrStumpByData = scoringDbRepository.findScoringDBByLookup(
                    lookUpMaker(match, catchOrStumpBy)
            );
            if (Objects.nonNull(batter) && Objects.isNull(batterData)) {
                scoringDbRepository.save(
                        ScoringDB
                                .builder()
                                .lookup(lookUpMaker(match, batter))
                                .playerName(batter.getName())
                                .playerId(batter.getId())
                                .matchNo(match.getMatch())
                                .batting(true)
                                .bowling(false)
                                .runs(o.getRuns())
                                .ballsFaced(o.getBalls())
                                .fours(o.getFours())
                                .sixes(o.getSixes())
                                .duck(o.getRuns() == 0 && o.isOut())
                                .build()
                );
            } else if (Objects.nonNull(batterData)) {
                batterData.setBatting(true);
                batterData.setRuns(o.getRuns());
                batterData.setBallsFaced(o.getBalls());
                batterData.setFours(o.getFours());
                batterData.setSixes(o.getSixes());
                batterData.setDuck(o.getRuns() == 0 && o.isOut());
                scoringDbRepository.save(batterData);
            }
            if (
                    Objects.nonNull(catchOrStumpBy) && Objects.isNull(catchOrStumpByData)
            ) {
                scoringDbRepository.save(
                        ScoringDB
                                .builder()
                                .lookup(lookUpMaker(match, catchOrStumpBy))
                                .playerName(catchOrStumpBy.getName())
                                .playerId(catchOrStumpBy.getId())
                                .matchNo(match.getMatch())
                                .catchOrStumps(catchStump.get(lookUpMaker(match, catchOrStumpBy)))
                                .build()
                );
            } else if (Objects.nonNull(catchOrStumpByData)) {
                catchOrStumpByData.setCatchOrStumps(
                        catchStump.get(lookUpMaker(match, catchOrStumpBy))
                );
                scoringDbRepository.save(catchOrStumpByData);
            }
        });
        bowlingSession.forEach(o -> {
            Player bowler = o.getBowlerName();
            ScoringDB bowlerData = scoringDbRepository.findScoringDBByLookup(
                    lookUpMaker(match, bowler)
            );
            if (Objects.nonNull(bowler) && Objects.isNull(bowlerData)) {
                scoringDbRepository.save(
                        ScoringDB
                                .builder()
                                .lookup(lookUpMaker(match, bowler))
                                .playerName(bowler.getName())
                                .playerId(bowler.getId())
                                .matchNo(match.getMatch())
                                .batting(false)
                                .bowling(true)
                                .overs(o.getOvers())
                                .dots(o.getDots())
                                .runsConceded(o.getRuns())
                                .wickets(o.getWickets())
                                .build()
                );
            } else if (Objects.nonNull(bowlerData)) {
                bowlerData.setBowling(true);
                bowlerData.setOvers(o.getOvers());
                bowlerData.setRunsConceded(o.getRuns());
                bowlerData.setWickets(o.getWickets());
                bowlerData.setDots(o.getDots());
                scoringDbRepository.save(bowlerData);
            }
        });
        //score flags
        scoreDBFlagProcessor(match.getMatch());
        //update points table
        pointsDBProcessScore(match.getMatch());
        return ResponseEntity.ok(true);
    }

    @GetMapping("/findSubstitutionByUsername")
    public ResponseEntity<?> findSubstitutionByUsername(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "matchNo") String matchNo
    ) {
        return ResponseEntity.ok(
                substitutionRepository.findById(pointsLookup(matchNo, username))
        );
    }

    @GetMapping("/updateIPLTeamListFromCricInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateIPLTeamListFromCricInfo() {
        try{
            Map<String, Object> data = this.cricInfoService.getIPLTeams("en", SeriesID);
            Map<String, Object> content = (Map<String, Object>) data.get("content");
            ArrayList<Map<String, Object>> teamsList = (ArrayList<Map<String, Object>>) content.get("teams");
            ArrayList<Team> fantasticTeamList = new ArrayList<Team>();
            teamsList.forEach(p->{
                Map<String, Object> o = (Map<String, Object>) p.get("team");
                fantasticTeamList.add(Team.builder()
                        .teamId((Integer) o.get("id"))
                        .name((String)o.get("longName"))
                        .teamAbbr((String)o.get("name"))
                        .logoUrl((String)o.get("imageUrl"))
                        .build());
            });
            List<Team> temp = this.teamRepository.saveAll(fantasticTeamList);
            System.out.println(temp);
           return ResponseEntity.ok(
                    true
            );

        }catch (Exception e){
            return ResponseEntity.ok(
                    false
            );
        }
    }

    @GetMapping("/updateIPLPlayersFromCricInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateIPLPlayersFromCricInfo() {
        try{
            Map<String, Object> data = this.cricInfoService.getIPLSquads("en", SeriesID);
            Map<String, Object> squadContent = (Map<String, Object>) data.get("content");
            ArrayList<Map<String, Object>> squadsList = (ArrayList<Map<String, Object>>) squadContent.get("squads");
            squadsList.forEach(p->{
                Map<String, Object> o = (Map<String, Object>) p.get("squad");
                Optional<Team> team = teamRepository.findByTeamId((Integer) o.get("teamId"));
                Map<String, Object> teamSquad = this.cricInfoService.getIPLSquadDetailsBySquadId("en", SeriesID, (Integer) o.get("objectId"));
                Map<String, Object> squadTeamContent = (Map<String, Object>) teamSquad.get("content");
                Map<String, Object> squadDetails = (Map<String, Object>) squadTeamContent.get("squadDetails");
                ArrayList<Map<String, Object>> playersList = (ArrayList<Map<String, Object>>) squadDetails.get("players");
                Map<String, Object> squad = (Map<String, Object>) squadDetails.get("squad");
                ArrayList<Player> plrList = new ArrayList<>();
                playersList.forEach(player->{
                    Map<String, Object> plr = (Map<String, Object>) player.get("player");
                    plrList.add(Player.builder().team(((String) squad.get("title")).replaceAll(" Squad", ""))
                                    .id(((Integer) plr.get("id")).longValue())
                                    .name((String) plr.get("longName"))
                                    .active("ACTIVE")
                                    .teamId((Integer) squad.get("teamId"))
                                    .imageUrl((String) plr.get("imageUrl"))

                            .build());

                });
                this.playerRepository.saveAll(plrList);
                team.ifPresent(t->{
                    t.setSquadId((Integer) o.get("objectId"));
                    teamRepository.save(t);
                });
            });
            return ResponseEntity.ok(
                    true
            );

        }catch (Exception e){
            System.out.println(e);
            return ResponseEntity.ok(
                    false
            );
        }
    }

    @GetMapping("/updateIPLAllDetailsFromCricInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateIPLAllDetailsFromCricInfo() {
        try{
            ///////////////////Team Initial list population//////////////////////////////
            Map<String, Object> data1 = this.cricInfoService.getIPLTeams("en", SeriesID);
            Map<String, Object> content = (Map<String, Object>) data1.get("content");
            ArrayList<Map<String, Object>> teamsList = (ArrayList<Map<String, Object>>) content.get("teams");
            ArrayList<Team> fantasticTeamList = new ArrayList<Team>();
            teamsList.forEach(p->{
                Map<String, Object> o = (Map<String, Object>) p.get("team");
                fantasticTeamList.add(Team.builder()
                        .teamId((Integer) o.get("id"))
                        .name((String)o.get("longName"))
                        .teamAbbr((String)o.get("name"))
                        .logoUrl((String)o.get("imageUrl"))
                        .build());
            });
            /////////////////////////Squad List////////////////////////////
            Map<String, Object> data = this.cricInfoService.getIPLSquads("en", SeriesID);
            Map<String, Object> squadContent = (Map<String, Object>) data.get("content");
            ArrayList<Map<String, Object>> squadsList = (ArrayList<Map<String, Object>>) squadContent.get("squads");
            squadsList.forEach(p->{
                Map<String, Object> o = (Map<String, Object>) p.get("squad");
                Map<String, Object> teamSquad = this.cricInfoService.getIPLSquadDetailsBySquadId("en", SeriesID, (Integer) o.get("objectId"));
                Map<String, Object> squadTeamContent = (Map<String, Object>) teamSquad.get("content");
                Map<String, Object> squadDetails = (Map<String, Object>) squadTeamContent.get("squadDetails");
                ArrayList<Map<String, Object>> playersList = (ArrayList<Map<String, Object>>) squadDetails.get("players");
                Map<String, Object> squad = (Map<String, Object>) squadDetails.get("squad");
                ArrayList<Player> plrList = new ArrayList<>();
                Optional<Team> team = fantasticTeamList.stream().filter(op-> Objects.equals(op.getTeamId(), (Integer) (squad.get("teamId")))).findAny();
                playersList.forEach(player->{
                    Map<String, Object> plr = (Map<String, Object>) player.get("player");
                    plrList.add(Player.builder().team(((String) squad.get("title")).replaceAll(" Squad", ""))
                            .id(((Integer) plr.get("id")).longValue())
                            .name((String) plr.get("longName"))
                            .active("active")
                            .teamId((Integer) squad.get("teamId"))
                            .imageUrl((String) plr.get("imageUrl"))

                            .build());

                });
                this.playerRepository.saveAll(plrList);
                team.ifPresent(t->{
                    t.setSquadId((Integer) o.get("objectId"));
                    teamRepository.save(t);
                });
            });
            // team update
            this.teamRepository.saveAll(fantasticTeamList);
            //schedule update
            Map<String, Object> data2 = this.cricInfoService.getIPLSchedule("en", SeriesID);
            Map<String, Object> content2 = (Map<String, Object>) data2.get("content");
            ArrayList<Map<String, Object>> matchList = (ArrayList<Map<String, Object>>) content2.get("matches");
            ArrayList<Tournament> fantasticMatchList = new ArrayList<>();
            List<Tournament> existingTournamentsDBs = tournamentRepository.findAll();
            matchList.forEach(p->{
                Map<String, Object> groundDetails = (Map<String, Object>) p.get("ground");
                ArrayList<Map<String, Object>> teams = (ArrayList<Map<String, Object>>) p.get("teams");
                Map<String, Object> team1 = (Map<String, Object>) teams.get(0).get("team");
                Map<String, Object> team2 = (Map<String, Object>) teams.get(1).get("team");
                int idInt = (Integer) p.get("scribeId");
                Long idLong = (long) idInt;
                Optional<Tournament> existingTournamentsDB = existingTournamentsDBs.stream()
                        .filter(o -> o.getId().equals(idLong))
                        .findFirst();
                if(existingTournamentsDB.isPresent()){
                    Tournament t = existingTournamentsDB.get();
                    fantasticMatchList.add(
                            Tournament.builder()
                                    .id(t.getId())
                                    .matchNo(t.getMatchNo())
                                    .enable11(t.getEnable11())
                                    .completed(t.getCompleted())
                                    .cricInfoURL("https://hs-consumer-api.espncricinfo.com/v1/pages/match/home?lang=en&seriesId="+SeriesID+"&matchId="+idLong)
                                    .cricInfoId(idInt)
                                    .venue((String) groundDetails.get("longName"))
                                    .matchdate(extractDate((String) p.get("startDate")))
                                    .started(t.getStarted())
                                    .abandoned(t.getAbandoned())
                                    .unlimitSubstitution(t.getUnlimitSubstitution())
                                    .matchtime(extractTime((String) p.get("startTime")))
                                    .team1((String) team1.get("longName"))
                                    .team2((String) team2.get("longName"))
                                    .build());
                }
                else fantasticMatchList.add(Tournament.builder()
                        .id(idLong)
                        .matchNo(extractNumber((String) p.get("title")))
                        .enable11(false)
                        .completed(false)
                        .cricInfoURL("https://hs-consumer-api.espncricinfo.com/v1/pages/match/home?lang=en&seriesId="+SeriesID+"&matchId="+idLong)
                        .cricInfoId(idInt)
                        .venue((String) groundDetails.get("longName"))
                        .matchdate(extractDate((String) p.get("startDate")))
                        .started(false)
                        .abandoned(false)
                        .unlimitSubstitution(false)
                        .matchtime(extractTime((String) p.get("startTime")))
                        .team1((String) team1.get("longName"))
                        .team2((String) team2.get("longName"))
                        .build());
            });
            this.tournamentRepository.saveAll(fantasticMatchList);
            return ResponseEntity.ok(
                    true
            );

        }catch (Exception e){
            System.out.println(e);
            return ResponseEntity.ok(
                    false
            );
        }
    }
    @GetMapping("/updateIPLscheduleFromCricInfo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateIPLscheduleFromCricInfo() {
        try{
            Map<String, Object> data = this.cricInfoService.getIPLSchedule("en", SeriesID);
            Map<String, Object> content = (Map<String, Object>) data.get("content");
            ArrayList<Map<String, Object>> matchList = (ArrayList<Map<String, Object>>) content.get("matches");
            ArrayList<Tournament> fantasticMatchList = new ArrayList<>();
            List<Tournament> existingTournamentsDBs = tournamentRepository.findAll();
            matchList.forEach(p->{
                ArrayList<Map<String, Object>> teams = (ArrayList<Map<String, Object>>) p.get("teams");
                Map<String, Object> team1 = (Map<String, Object>) teams.get(0).get("team");
                Map<String, Object> team2 = (Map<String, Object>) teams.get(1).get("team");
                int idInt = (Integer) p.get("id");
                Long idLong = (long) idInt;
                Optional<Tournament> existingTournamentsDB = existingTournamentsDBs.stream()
                        .filter(o -> o.getId().equals(p.get("id")))
                        .findFirst();
                if(existingTournamentsDB.isPresent()){
                    Tournament t = existingTournamentsDB.get();
                    fantasticMatchList.add(
                            Tournament.builder()
                            .id(t.getId())
                            .matchNo(t.getMatchNo())
                            .enable11(t.getEnable11())
                            .completed(t.getCompleted())
                            .cricInfoURL("")
                            .cricInfoId(idInt)
                            .venue("")
                            .matchdate(extractDate((String) p.get("startDate")))
                            .started(t.getStarted())
                            .abandoned(t.getAbandoned())
                            .unlimitSubstitution(t.getUnlimitSubstitution())
                            .matchtime(extractTime((String) p.get("startTime")))
                            .team1((String) team1.get("longName"))
                            .team2((String) team2.get("longName"))
                            .build());
                }
               else fantasticMatchList.add(Tournament.builder()
                                .id(idLong)
                                .matchNo(extractNumber((String) p.get("title")))
                                .enable11(false)
                                .completed(false)
                                .cricInfoURL("")
                                .cricInfoId(idInt)
                                .venue("")
                                .matchdate(extractDate((String) p.get("startDate")))
                                .started(false)
                                .abandoned(false)
                                .unlimitSubstitution(false)
                                .matchtime(extractTime((String) p.get("startTime")))
                                .team1((String) team1.get("longName"))
                                .team2((String) team2.get("longName"))
                        .build());
            });
            List<Tournament> temp = this.tournamentRepository.saveAll(fantasticMatchList);
            System.out.println(temp);
            return ResponseEntity.ok(
                    true
            );

        }catch (Exception e){
            System.out.println(e.toString());
            return ResponseEntity.ok(
                    false
            );
        }
    }
    public String extractDate(String timestamp) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy");
        try {
            Date date = inputFormat.parse(timestamp);
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    public String extractTime(String timestamp) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        try {
            Date date = inputFormat.parse(timestamp);
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }
    public String extractNumber(String str) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(str);

        if (matcher.find()) {
            return matcher.group();
        }

        // Return empty string if no number is found
        return "";
    }
    private String lookUpMaker(InningsSession match, Player a) {
        if (Objects.isNull(a) || Objects.isNull(a.getId())) {
            System.out.println(a);
            return "";
        }
        return match.getMatch().trim().concat("_").concat(Long.toString(a.getId()));
    }

    private void pointsDBProcessScore(String matchNo) {
        List<Points> pointsDatas = pointsRepository.findPointsByMatchNo(matchNo);
        List<ScoringDB> matchScoreDBs = scoringDbRepository.findScoringDBSByMatchNo(
                matchNo
        );
        Map<String, Substitution> subMap = substitutionRepository
                .findAllByMatchNo(matchNo)
                .stream()
                .collect(Collectors.toMap(Substitution::getLookUp, o -> o));
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> batMap = new HashMap<>();
        Map<String, Integer> bowlMap = new HashMap<>();
        for (ScoringDB score : matchScoreDBs) {
            map.put(score.getLookup(), score.getTotalPlayerPoint());
            batMap.put(score.getLookup(), score.getBattingPoints());
            bowlMap.put(score.getLookup(), score.getBowlingPoints());
        }
        for (Points p : pointsDatas) {
            Substitution sub = subMap.get(p.getLookUp());
            double total = 0;
            if (map.get(lookup2(matchNo, p.getCaptain())) != null) {
                p.setCaptainPoint(map.get(lookup2(matchNo, p.getCaptain())) * 2.0);
                total = total + p.getCaptainPoint();
            }
            if (map.get(lookup2(matchNo, p.getVcaptain())) != null) {
                p.setVcaptainPoint(map.get(lookup2(matchNo, p.getVcaptain())) * 1.5);
                total = total + p.getVcaptainPoint();
            }
            if (batMap.get(lookup2(matchNo, p.getBattinghero())) != null) {
                p.setBattingheroPoint(
                        map.get(lookup2(matchNo, p.getBattinghero())) *
                                1.0 +
                                batMap.get(lookup2(matchNo, p.getBattinghero())) *
                                        0.5
                );
                total = total + p.getBattingheroPoint();
            }
            if (bowlMap.get(lookup2(matchNo, p.getBowlinghero())) != null) {
                p.setBowlingheroPoint(
                        map.get(lookup2(matchNo, p.getBowlinghero())) *
                                1.0 +
                                bowlMap.get(lookup2(matchNo, p.getBowlinghero())) *
                                        0.5
                );
                total = total + p.getBowlingheroPoint();
            }
            if (map.get(lookup2(matchNo, p.getPlayer5())) != null) {
                p.setPlayer5Point(map.get(lookup2(matchNo, p.getPlayer5())) * 1.0);
                total = total + p.getPlayer5Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer6())) != null) {
                p.setPlayer6Point(map.get(lookup2(matchNo, p.getPlayer6())) * 1.0);
                total = total + p.getPlayer6Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer7())) != null) {
                p.setPlayer7Point(map.get(lookup2(matchNo, p.getPlayer7())) * 1.0);
                total = total + p.getPlayer7Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer8())) != null) {
                p.setPlayer8Point(map.get(lookup2(matchNo, p.getPlayer8())) * 1.0);
                total = total + p.getPlayer8Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer9())) != null) {
                p.setPlayer9Point(map.get(lookup2(matchNo, p.getPlayer9())) * 1.0);
                total = total + p.getPlayer9Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer10())) != null) {
                p.setPlayer10Point(map.get(lookup2(matchNo, p.getPlayer10())) * 1.0);
                total = total + p.getPlayer10Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer11())) != null) {
                p.setPlayer11Point(map.get(lookup2(matchNo, p.getPlayer11())) * 1.0);
               // total = total + p.getPlayer11Point();
            }
            if (map.get(lookup2(matchNo, p.getPlayer12())) != null) {
                p.setPlayer12Point(map.get(lookup2(matchNo, p.getPlayer12())) * 1.0);
               // total = total + p.getPlayer12Point();
            }
            //In 2024 edition - only one imapct sub score will be taken - better one
            if (Double.compare(p.getPlayer11Point(), p.getPlayer12Point()) == 0) {
                total = total + p.getPlayer11Point();
                p.setPlayer12Point(0.0);
            }
            else if (Double.compare(p.getPlayer11Point(), p.getPlayer12Point()) < 0) {
                total = total + p.getPlayer12Point();
                p.setPlayer11Point(0.0);
            }
            else {
                total = total + p.getPlayer11Point();
                p.setPlayer12Point(0.0);
            }
            if (
                    !Objects.isNull(sub) &&
                            sub.getTotal() != null &&
                            sub.getTotal() >= 0 &&
                            sub.getUsed() != null &&
                            sub.getUsed() >= 0 &&
                            sub.getUsed() > sub.getTotal()
            ) {
                p.setOverSubNegativePoints(overSubNegativeCalc(sub.getTotal() - sub.getUsed()));
                total = total + p.getOverSubNegativePoints();
            }
            p.setTotal(total);
        }
        pointsDatas.sort(Collections.reverseOrder());
        int rank = 0;
        for (Points p : pointsDatas) {
            p.setRank_no(++rank);
        }
        pointsRepository.saveAll(pointsDatas);
    }
    private Double overSubNegativeCalc(Integer extraSub) {
        double returnVal = 0.0;
        if (extraSub < 1) {
            // Return 0 for values less than 1
            returnVal = 0.0;
        } else if (extraSub > 9) {
            // Return the value times -250 for values greater than 9
            returnVal = -250.0 * extraSub;
        } else {
            // Calculate the return value based on the switch cases
            switch (extraSub) {
                case 1:
                    returnVal = -25.0;
                    break;
                case 2:
                    returnVal = -100.0;
                    break;
                case 3:
                    returnVal = -225.0;
                    break;
                case 4:
                    returnVal = -400.0;
                    break;
                case 5:
                    returnVal = -625.0;
                    break;
                case 6:
                    returnVal = -900.0;
                    break;
                case 7:
                    returnVal = -1400.0;
                    break;
                case 8:
                    returnVal = -1600.0;
                    break;
                case 9:
                    returnVal = -1800.0;
                    break;
            }
        }
        return returnVal;
    }


    private String lookup2(String matchNo, Long playerID) {
        return matchNo.trim().trim().concat("_").concat(Long.toString(playerID));
    }

    private String pointsLookup(String matchNo, String username) {
        return matchNo.trim().trim().concat("_").concat(username);
    }

    private void scoreDBFlagProcessor(String matchNo) {
        List<ScoringDB> matchScoreDB = scoringDbRepository.findScoringDBSByMatchNo(
                matchNo
        );
        matchScoreDB.forEach(o -> {
            //score more than 30
            if (Objects.nonNull(o.getRuns()) && o.getRuns() >= 30) {
                o.setScore30(1);
            }
            //score more than 50
            if (Objects.nonNull(o.getRuns()) && o.getRuns() >= 50) {
                o.setScore50(1);
            }
            //score more than 100
            if (Objects.nonNull(o.getRuns()) && o.getRuns() >= 100) {
                o.setScore100(1);
            }
            //score more than 150
            if (Objects.nonNull(o.getRuns()) && o.getRuns() >= 150) {
                o.setScore150(1);
            }
            //3 wickets
            if (Objects.nonNull(o.getWickets()) && o.getWickets() >= 3) {
                o.setWicket3(1);
            }
            //4 wickets
            if (Objects.nonNull(o.getWickets()) && o.getWickets() >= 4) {
                o.setWicket4(1);
            }
            //5 wickets
            if (Objects.nonNull(o.getWickets()) && o.getWickets() >= 5) {
                o.setWicket5(1);
            }
            //6 wickets
            if (Objects.nonNull(o.getWickets()) && o.getWickets() >= 6) {
                o.setWicket6(1);
            }
            //Strike Rate
            if (
                    Objects.nonNull(o.getRuns()) &&
                            Objects.nonNull(o.getBallsFaced()) &&
                            o.getBallsFaced() >= 6
            ) {
                o.setStrikeRate(strikerate(o.getBallsFaced(), o.getRuns()));
            }
            // Economy
            if (
                    Objects.nonNull(o.getRunsConceded()) &&
                            Objects.nonNull(o.getOvers()) &&
                            o.getOvers() >= 1.0
            ) {
                o.setEconomy(economy(o.getOvers(), o.getRunsConceded()));
            }
            //---------------Batter-------------------
            //is run scored ?
            if (Objects.nonNull(o.getRuns()) && o.getRuns() > 0) {
                o.setRunScoreBonus(o.getRuns());
            }
            //4s & 6s bonus
            if (Objects.nonNull(o.getFours()) || Objects.nonNull(o.getSixes())) {
                int bonus = Objects.nonNull(o.getFours()) ? o.getFours() * 2 : 0;
                bonus = bonus + (Objects.nonNull(o.getSixes()) ? o.getSixes() * 4 : 0);
                o.setFoursAndSixesBonus(bonus);
            }
            //30,50,100,150 runs
            if (
                    Objects.nonNull(o.getScore30()) ||
                            Objects.nonNull(o.getScore50()) ||
                            Objects.nonNull(o.getScore100()) ||
                            Objects.nonNull(o.getScore150())
            ) {
                int bonus = Objects.nonNull(o.getScore30()) ? o.getScore30() * 5 : 0;
                bonus =
                        bonus + (Objects.nonNull(o.getScore50()) ? o.getScore50() * 10 : 0);
                bonus =
                        bonus + (Objects.nonNull(o.getScore100()) ? o.getScore100() * 50 : 0);
                bonus =
                        bonus +
                                (Objects.nonNull(o.getScore150()) ? o.getScore150() * 100 : 0);
                o.setBonusFor30_50_100_150(bonus);
            }
            //Duck Bonus
            if (Objects.nonNull(o.getDuck())) {
                int bonus = o.getDuck() ? -5 : 0;
                o.setDuckBonus(bonus);
            }
            //SR bonus
            if (Objects.nonNull(o.getStrikeRate())) {
                double P2 = o.getStrikeRate();
                int bonus = 0;
                if (P2 > 170) {
                    bonus = 6;
                } else if (P2 > 150) {
                    bonus = 4;
                } else if (P2 >= 130) {
                    bonus = 2;
                } else if (P2 >= 60 && P2 <= 70) {
                    bonus = -2;
                } else if (P2 >= 50 && P2 < 60) {
                    bonus = -4;
                } else if (P2 < 50) {
                    bonus = -6;
                }
                o.setSRBonus(bonus);
            }
            //batting bonus - batting hero
            o.setBattingPoints(
                    (o.getRunScoreBonus() != null ? o.getRunScoreBonus() : 0) +
                            (o.getFoursAndSixesBonus() != null ? o.getFoursAndSixesBonus() : 0) +
                            (
                                    o.getBonusFor30_50_100_150() != null
                                            ? o.getBonusFor30_50_100_150()
                                            : 0
                            ) +
                            (o.getDuckBonus() != null ? o.getDuckBonus() : 0) +
                            (o.getSRBonus() != null ? o.getSRBonus() : 0)
            );

            //--------------Bowler--------------
            //Wkts Bonus
            if (Objects.nonNull(o.getWickets()) && o.getWickets() > 0) {
                o.setWicketBonus(o.getWickets() * 25);
            }
            //Dot Bonus
            if (Objects.nonNull(o.getDots()) && o.getDots() > 0) {
                int bonus = o.getDots() * 3;
                o.setDotBonus(bonus);
            }
            //3 wkts,4wkts,5wkts,6wkts bonus
            if (
                    Objects.nonNull(o.getWicket3()) ||
                            Objects.nonNull(o.getWicket4()) ||
                            Objects.nonNull(o.getWicket5()) ||
                            Objects.nonNull(o.getWicket6())
            ) {
                int bonus = Objects.nonNull(o.getWicket3()) ? o.getWicket3() * 12 : 0;
                bonus =
                        bonus + (Objects.nonNull(o.getWicket4()) ? o.getWicket4() * 16 : 0);
                bonus =
                        bonus + (Objects.nonNull(o.getWicket5()) ? o.getWicket5() * 20 : 0);
                bonus =
                        bonus + (Objects.nonNull(o.getWicket6()) ? o.getWicket6() * 30 : 0);
                o.setBonusFor3wk4wk5wk6wk(bonus);
            }
            //Catch Bonus
            if (Objects.nonNull(o.getCatchOrStumps()) && o.getCatchOrStumps() > 0) {
                o.setCatchBonus(o.getCatchOrStumps() * 4);
            }

            //Economy bonus
            if (Objects.nonNull(o.getEconomy()) && o.getEconomy() > 0) {
                double U2 = o.getEconomy();
                int bonus = 0;
                if (U2 <= 5) {
                    bonus = 6;
                } else if (U2 <= 7) {
                    bonus = 4;
                } else if (U2 >= 10 && U2 <= 12) {
                    bonus = -4;
                } else if (U2 > 12) {
                    bonus = -6;
                }
                o.setEconomyBonus(bonus);
            }
            //bowling points
            o.setBowlingPoints(
                    (o.getWicketBonus() != null ? o.getWicketBonus() : 0) +
                            (o.getDotBonus() != null ? o.getDotBonus() : 0) +
                            (
                                    o.getBonusFor3wk4wk5wk6wk() != null ? o.getBonusFor3wk4wk5wk6wk() : 0
                            ) +
                            (o.getEconomyBonus() != null ? o.getEconomyBonus() : 0)
            );
            //total score
            int totalScrore = 0;
            if (Objects.nonNull(o.getRunScoreBonus())) {
                totalScrore = totalScrore + o.getRunScoreBonus();
            }
            if (Objects.nonNull(o.getFoursAndSixesBonus())) {
                totalScrore = totalScrore + o.getFoursAndSixesBonus();
            }
            if (Objects.nonNull(o.getBonusFor30_50_100_150())) {
                totalScrore = totalScrore + o.getBonusFor30_50_100_150();
            }
            if (Objects.nonNull(o.getSRBonus())) {
                totalScrore = totalScrore + o.getSRBonus();
            }
            if (Objects.nonNull(o.getDuckBonus())) {
                totalScrore = totalScrore + o.getDuckBonus();
            }
            if (Objects.nonNull(o.getWicketBonus())) {
                totalScrore = totalScrore + o.getWicketBonus();
            }
            if (Objects.nonNull(o.getDotBonus())) {
                totalScrore = totalScrore + o.getDotBonus();
            }
            if (Objects.nonNull(o.getBonusFor3wk4wk5wk6wk())) {
                totalScrore = totalScrore + o.getBonusFor3wk4wk5wk6wk();
            }
            if (Objects.nonNull(o.getEconomyBonus())) {
                totalScrore = totalScrore + o.getEconomyBonus();
            }
            if (Objects.nonNull(o.getCatchBonus())) {
                totalScrore = totalScrore + o.getCatchBonus();
            }
            o.setTotalPlayerPoint(totalScrore);
        });

        scoringDbRepository.saveAll(matchScoreDB);
    }

    private double strikerate(Integer ballsFaced, Integer runs) {
        double ab = (double) runs / ballsFaced;
        return ab * 100;
    }

    private Double economy(Double overs, Integer runsCon) {
        if (Objects.isNull(overs) || overs < 1) return 0.0D;
        String[] arr = String.valueOf(overs).split("\\.");
        if (arr.length == 2) {
            int fullOver = Integer.parseInt(arr[0]);
            int balls = Integer.parseInt(arr[1]);
            int temp = fullOver * 6 + balls;
            Double normalizedOver = (double) temp / 6.0;
            return Double.valueOf(decfor.format((runsCon * 1.0) / normalizedOver));
        } else return Double.valueOf(decfor.format(runsCon * 1.0 / overs));
    }

    public Optional<User> findGuestByToken(String bear) {
        return getGuests(bear, jwtProvider);
    }

    private Optional<User> getGuests(String bear, JwtProvider jwtProvider) {
        if (bear.equals("")) return Optional.empty();
        String token = bear.replaceAll("Bearer ", "").trim();
        String user = jwtProvider.getUserNameFromJwtToken(token);
        return userRepository.findByUsername(user);
    }

    private String calculateChanges(Points points){
        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        String pattern = "yyyy-MM-dd HH:mm:ss";
        String date = localDateTime.format(DateTimeFormatter.ofPattern(pattern));
        Map<Long, String> mapCurrentPoints = new HashMap<>();
        Map<Long, String> mapLastPoints = new HashMap<>();
        StringBuilder stb = new StringBuilder();
        stb.append("Audit TimeCurrent Local Time: "+date).append("\r\n");
        mapCurrentPoints.put(points.getCaptain(), "captain");
        mapCurrentPoints.put(points.getVcaptain(), "vcaptain");
        mapCurrentPoints.put(points.getBowlinghero(), "bowlinghero");
        mapCurrentPoints.put(points.getBattinghero(), "battinghero");
        mapCurrentPoints.put(points.getPlayer5(), "player5");
        mapCurrentPoints.put(points.getPlayer6(), "player6");
        mapCurrentPoints.put(points.getPlayer7(), "player7");
        mapCurrentPoints.put(points.getPlayer8(), "player8");
        mapCurrentPoints.put(points.getPlayer9(), "player9");
        mapCurrentPoints.put(points.getPlayer10(), "player10");
        mapCurrentPoints.put(points.getPlayer11(), "player11");
        mapCurrentPoints.put(points.getPlayer12(), "player12");


        Optional<Points> lastUpdatedPointsOpt = pointsRepository.findPointsInDescLastUpdatedPoints(points.getUsername()).stream().findFirst();
        if(lastUpdatedPointsOpt.isPresent()){
            Points lastUpdatedPoints = lastUpdatedPointsOpt.get();
            mapLastPoints.put(lastUpdatedPoints.getCaptain(), "captain");
            mapLastPoints.put(lastUpdatedPoints.getVcaptain(), "vcaptain");
            mapLastPoints.put(lastUpdatedPoints.getBowlinghero(), "bowlinghero");
            mapLastPoints.put(lastUpdatedPoints.getBattinghero(), "battinghero");
            mapLastPoints.put(lastUpdatedPoints.getPlayer5(), "player5");
            mapLastPoints.put(lastUpdatedPoints.getPlayer6(), "player6");
            mapLastPoints.put(lastUpdatedPoints.getPlayer7(), "player7");
            mapLastPoints.put(lastUpdatedPoints.getPlayer8(), "player8");
            mapLastPoints.put(lastUpdatedPoints.getPlayer9(), "player9");
            mapLastPoints.put(lastUpdatedPoints.getPlayer10(), "player10");
            mapLastPoints.put(lastUpdatedPoints.getPlayer11(), "player11");
            mapLastPoints.put(lastUpdatedPoints.getPlayer12(), "player12");
            Set<Long> allIds = new HashSet<>();
            allIds.addAll(mapCurrentPoints.keySet());
            allIds.addAll(mapLastPoints.keySet());
            Map<Long, Player> playerMap = new HashMap<>();
            playerRepository.findByIdIsIn(new ArrayList<>(allIds)).forEach(o->{
                playerMap.put(o.getId(),o);
            });
           if(!Objects.equals(lastUpdatedPoints.getCaptain(),points.getCaptain())){
               stb.append("captain changed from :").append(playerMap.get(lastUpdatedPoints.getCaptain()).getName()).append("->").append(playerMap.get(points.getCaptain()).getName());
               stb.append("\r\n");
           }
            if(!Objects.equals(lastUpdatedPoints.getVcaptain(),points.getVcaptain())){
                stb.append("vcaptain changed from :").append(playerMap.get(lastUpdatedPoints.getVcaptain()).getName()).append("->").append(playerMap.get(points.getVcaptain()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getBowlinghero(),points.getBowlinghero())){
                stb.append("bowlinghero changed from :").append(playerMap.get(lastUpdatedPoints.getBowlinghero()).getName()).append("->").append(playerMap.get(points.getBowlinghero()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getBattinghero(),points.getBattinghero())){
                stb.append("battinghero changed from :").append(playerMap.get(lastUpdatedPoints.getBattinghero()).getName()).append("->").append(playerMap.get(points.getBattinghero()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer5(),points.getPlayer5())){
                stb.append("player5 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer5()).getName()).append("->").append(playerMap.get(points.getPlayer5()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer6(),points.getPlayer6())){
                stb.append("player6 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer6()).getName()).append("->").append(playerMap.get(points.getPlayer6()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer7(),points.getPlayer7())){
                stb.append("player7 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer7()).getName()).append("->").append(playerMap.get(points.getPlayer7()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer8(),points.getPlayer8())){
                stb.append("player8 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer8()).getName()).append("->").append(playerMap.get(points.getPlayer8()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer9(),points.getPlayer9())){
                stb.append("player9 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer9()).getName()).append("->").append(playerMap.get(points.getPlayer9()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer10(),points.getPlayer10())){
                stb.append("player10 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer10()).getName()).append("->").append(playerMap.get(points.getPlayer10()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer11(),points.getPlayer11())){
                stb.append("player11 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer11()).getName()).append("->").append(playerMap.get(points.getPlayer11()).getName());
                stb.append("\r\n");
            }
            if(!Objects.equals(lastUpdatedPoints.getPlayer12(),points.getPlayer12())){
                stb.append("player12 changed from :").append(playerMap.get(lastUpdatedPoints.getPlayer12()).getName()).append("->").append(playerMap.get(points.getPlayer12()).getName());
                stb.append("\r\n");
            }
        }else{
            Set<Long> allIds = new HashSet<>();
            allIds.addAll(mapCurrentPoints.keySet());
            allIds.addAll(mapLastPoints.keySet());
            Map<Long, Player> playerMap = new HashMap<>();
            playerRepository.findByIdIsIn(new ArrayList<>(allIds)).forEach(o->{
                playerMap.put(o.getId(),o);
            });

            stb.append("captain: ").append(playerMap.get(points.getCaptain()).getName()).append("\r\n");
            stb.append("vcaptain: ").append(playerMap.get(points.getVcaptain()).getName()).append("\r\n");
            stb.append("bowlinghero: ").append(playerMap.get(points.getBowlinghero()).getName()).append("\r\n");
            stb.append("battinghero: ").append(playerMap.get(points.getBattinghero()).getName()).append("\r\n");
            stb.append("player5: ").append(playerMap.get(points.getPlayer5()).getName()).append("\r\n");
            stb.append("player6: ").append(playerMap.get(points.getPlayer6()).getName()).append("\r\n");
            stb.append("player7: ").append(playerMap.get(points.getPlayer7()).getName()).append("\r\n");
            stb.append("player8: ").append(playerMap.get(points.getPlayer8()).getName()).append("\r\n");
            stb.append("player9: ").append(playerMap.get(points.getPlayer9()).getName()).append("\r\n");
            stb.append("player10: ").append(playerMap.get(points.getPlayer10()).getName()).append("\r\n");
            stb.append("player11: ").append(playerMap.get(points.getPlayer11()).getName()).append("\r\n");
            stb.append("player12: ").append(playerMap.get(points.getPlayer12()).getName()).append("\r\n");
        }
        return stb.toString();
    }
}
