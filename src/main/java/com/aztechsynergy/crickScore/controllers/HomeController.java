package com.aztechsynergy.crickScore.controllers;

import com.aztechsynergy.crickScore.dto.MatchDetailsDTO;
import com.aztechsynergy.crickScore.dto.TeamDTO;
import com.aztechsynergy.crickScore.model.*;
import com.aztechsynergy.crickScore.repository.*;
import com.aztechsynergy.crickScore.security.jwt.JwtProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path="/cricket")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 86400)
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
    @Autowired
    ScoringDbRepository scoringDbRepository;
    @Autowired
    PointsRepository pointsRepository;

    @Autowired
    SubstitutionRepository substitutionRepository;

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

    @GetMapping("/tournaments/{matchNo}")
    public ResponseEntity<?> getMatchDetailsByMatchNo(@PathVariable String matchNo) {
        return ResponseEntity.ok(tournamentRepository.findDistinctFirstByMatchNo(matchNo));
    }

    @GetMapping("/matchDetails")
    public ResponseEntity<?> matchDetails(@RequestParam String matchNo) {
        Tournament a = tournamentRepository.findDistinctFirstByMatchNo(matchNo);
        return ResponseEntity.ok(MatchDetailsDTO.builder()
                .matchNo(matchNo)
                .team1(TeamDTO.builder()
                        .name(a.getTeam1())
                        .players(playerRepository.findByTeam(a.getTeam1()) )
                        .build())
                .team2(TeamDTO.builder()
                        .name(a.getTeam2())
                        .players(playerRepository.findByTeam(a.getTeam2())  )
                        .build())
                .build());
    }

    @PutMapping("/updateMatchDetails")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMatchDetails(@RequestBody MatchDetailsDTO match) {

        playerRepository.saveAll(match.getTeam1().getPlayers());
        playerRepository.saveAll(match.getTeam2().getPlayers());

        return ResponseEntity.ok(true);
    }

    @PutMapping("/updateTournament")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTournament(@RequestBody Tournament match) {
        if(match.getEnable11()){
            List<Tournament> old = tournamentRepository.findAll().stream().peek(o -> o.setEnable11(false)).collect(Collectors.toList());
            tournamentRepository.saveAll(old);

            //from points
            List<String> totalPlayers = userRepository.findAllUserIds();
            List<String> matchUsers = pointsRepository.findUsersByMatchNo(match.getMatchNo());
            totalPlayers.removeAll(matchUsers);
            totalPlayers.forEach(p -> {
                Optional<Points> lastPoint = pointsRepository.findPointsInDescLastUpdatedPoints(p).stream().findFirst();
                if(lastPoint.isPresent()){
                    Points last = lastPoint.get();
                    last.setLastUpdatedTime(new Date());
                    pointsRepository.save(Points.builder().
                            lookUp(pointsLookup(match.getMatchNo(),p)).
                            matchNo(last.getMatchNo()).
                            username(last.getUsername()).
                            captain(last.getCaptain()).
                            vcaptain(last.getVcaptain()).
                            battinghero(last.getBattinghero()).
                            bowlinghero(last.getBowlinghero()).
                            player5(last.getPlayer5()).
                            player6(last.getPlayer6()).
                            player7(last.getPlayer7()).
                            player8(last.getPlayer8()).
                            player9(last.getPlayer9()).
                            player10(last.getPlayer10()).
                            player11(last.getPlayer11()).
                            player12(last.getPlayer12()).
                            lastUpdatedTime(last.getLastUpdatedTime()).
                            build());
                }

                Optional<Substitution> prevMatchSub = substitutionRepository.findById(pointsLookup(String.valueOf(Integer.parseInt(match.getMatchNo()) - 1), p));
                if(prevMatchSub.isPresent()){
                    Substitution prevSub = prevMatchSub.get();
                    int prevSubUnused = prevSub.getTotal() - prevSub.getUsed() >-1 ? prevSub.getTotal() - prevSub.getUsed() : 0;
                    Optional<Substitution> subMatch = substitutionRepository.findById(pointsLookup(match.getMatchNo(), p));
                    if(!subMatch.isPresent()){
                        substitutionRepository.save(Substitution.builder()
                                .lookUp(pointsLookup(match.getMatchNo(), p))
                                .username(p)
                                .matchNo(match.getMatchNo())
                                .free(2)
                                .total((2+prevSubUnused)>4 ? 2 : 2+prevSubUnused)
                                .used(0)
                                .build());
                    }
                }
            });
        }
        if(match.getStarted()){
            List<Tournament> old = tournamentRepository.findAll().stream().peek(o -> o.setStarted(false)).collect(Collectors.toList());
            tournamentRepository.saveAll(old);
        }
        tournamentRepository.save(match);

        return ResponseEntity.ok(tournamentRepository.save(match));
    }

    @GetMapping("/getActiveDream9Tournament")
    public ResponseEntity<?> getActiveDream9Tournament() {

        Optional<Tournament> data = tournamentRepository.findAll().stream().filter(Tournament::getEnable11).findFirst();

        return ResponseEntity.ok(data.orElse(null));
    }

    @GetMapping("/getStartedTournament")
    public ResponseEntity<?> getStartedTournament() {

        Optional<Tournament> data = tournamentRepository.findAll().stream().filter(Tournament::getStarted).findFirst();

        return ResponseEntity.ok(data.orElse(null));
    }

    @PostMapping("/updateDream9Details")
    public ResponseEntity<?> updateDream9Details(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear, @RequestBody Points points) {
        Optional<User> user = findGuestByToken(bear);
        if(!user.isPresent()) return ResponseEntity.ok(false);
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(points.getMatchNo());
        user.ifPresent(value -> points.setUsername(value.getUsername()));
        if( null == tournament || (tournament.getStarted() || !tournament.getEnable11())) return ResponseEntity.ok(false);
        points.setLookUp(pointsLookup(points.getMatchNo(),points.getUsername()));
        points.setLastUpdatedTime(new Date());
        pointsRepository.save(points);
        String infinteSubAvailable = substitutionRepository.findMatchByInfinitSubs(points.getUsername());
        if(!StringUtil.isNotBlank(infinteSubAvailable)){
            substitutionRepository.save(Substitution.builder()
                  .lookUp(pointsLookup(points.getMatchNo(),points.getUsername()))
                  .username(points.getUsername())
                  .matchNo(points.getMatchNo())
                  .free(100000)
                  .total(100000)
                  .used(0)
                  .build());
        }
        return ResponseEntity.ok(true);
    }

    @PostMapping("/updateSubstitutionsAndConfig")
    public ResponseEntity<?> updateSubstitutions(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear, @RequestBody Points points) {
        Optional<User> user = findGuestByToken(bear);
        if(!user.isPresent()) return ResponseEntity.ok(false);
        Tournament tournament = tournamentRepository.findDistinctFirstByMatchNo(points.getMatchNo());
        user.ifPresent(value -> points.setUsername(value.getUsername()));
        if( null == tournament || (tournament.getStarted() || !tournament.getEnable11())) return ResponseEntity.ok(false);
        points.setLookUp(pointsLookup(points.getMatchNo(),points.getUsername()));
        points.setLastUpdatedTime(new Date());
        Optional<Substitution> prevMatchSub = substitutionRepository.findById(pointsLookup(String.valueOf(Integer.parseInt(points.getMatchNo()) - 1), points.getUsername()));
        if(prevMatchSub.isPresent()){
            Substitution prevSub = prevMatchSub.get();
            int prevSubUnused = prevSub.getTotal() - prevSub.getUsed() >-1 ? prevSub.getTotal() - prevSub.getUsed() : 0;
            Optional<Substitution> subMatch = substitutionRepository.findById(pointsLookup(points.getMatchNo(), points.getUsername()));
            if(subMatch.isPresent()){
                Substitution subV = subMatch.get();
                subV.setFree(2);
                subV.setTotal((2+prevSubUnused)>4 ? 2 : 2+prevSubUnused);
                subV.setUsed(subV.getUsed()+1);
            }else{
                substitutionRepository.save(Substitution.builder()
                        .lookUp(pointsLookup(points.getMatchNo(), points.getUsername()))
                        .username(points.getUsername())
                        .matchNo(points.getMatchNo())
                        .free(2)
                        .total((2+prevSubUnused)>4 ? 2 : 2+prevSubUnused)
                        .used(1)
                        .build());
            }
        }
        else {
            Optional<Substitution> sub = substitutionRepository.findById(pointsLookup(points.getMatchNo(), points.getUsername()));
            String infiniteSubMatch = substitutionRepository.findMatchByInfinitSubs(points.getUsername());
            if(!sub.isPresent()){
                substitutionRepository.save(Substitution.builder()
                        .lookUp(pointsLookup(points.getMatchNo(), points.getUsername()))
                        .username(points.getUsername())
                        .matchNo(points.getMatchNo())
                        .free(!StringUtil.isNotBlank(infiniteSubMatch)? 100000 : 2)
                        .total(!StringUtil.isNotBlank(infiniteSubMatch)? 100000 : 2)
                        .used(1)
                        .build());
            }else{
                Substitution sub1 = sub.get();
                sub1.setUsed(sub1.getUsed()+1);
                substitutionRepository.save(sub1);
            }

        }

        pointsRepository.save(points);
        return ResponseEntity.ok(true);
    }
    @GetMapping("/getSubstitutionStatus/{matchNo}")
    public ResponseEntity<?> getSubstitutionStatus(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear, @PathVariable String matchNo) {
        Optional<User> user = findGuestByToken(bear);
        return user.map(value -> ResponseEntity.ok(substitutionRepository.findById(pointsLookup(matchNo, value.getUsername())).orElse(Substitution.builder().free(100000).total(100000).build()))).orElseGet(() -> ResponseEntity.ok(Substitution.builder().build()));
    }
    @GetMapping("/getDream9playerConfig/{matchNo}")
    public ResponseEntity<?> getDream9playerConfig(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear, @PathVariable String matchNo) {
        Optional<User> user = findGuestByToken(bear);
        if(user.isPresent()){
            Tournament t = tournamentRepository.findDistinctFirstByMatchNo(matchNo);
            if(!t.getEnable11()) return ResponseEntity.ok(false);
            List<Player> playerList = new ArrayList<>();
            Points points = pointsRepository.findByLookUp(pointsLookup(matchNo, user.get().getUsername()));
           if(points!=null){
               Map<Long,String> map = new HashMap<>();
               map.put(points.getCaptain(),"captain");
               map.put(points.getVcaptain() ,"vcaptain");
               map.put(points.getBowlinghero() ,"bowlinghero");
               map.put(points.getBattinghero() ,"battinghero");
               map.put(points.getPlayer5() ,"player5");
               map.put(points.getPlayer6() ,"player6");
               map.put(points.getPlayer7() ,"player7");
               map.put(points.getPlayer8() ,"player8");
               map.put(points.getPlayer9() ,"player9");
               map.put(points.getPlayer10() ,"player10");
               map.put(points.getPlayer11() ,"player11");
               map.put(points.getPlayer12() ,"player12");

               List<Long> listOfPlayerIds = new ArrayList<>(map.keySet());
               List<Player> players = playerRepository.findByIdIsIn(listOfPlayerIds);

               players.forEach(player->{
                   player.setAssignedRole(map.get(player.getId()));
                   playerList.add(player);
               });

               return ResponseEntity.ok(playerList);
           }
        }else {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(null);
    }
    @GetMapping("/getDream9Details")
    public ResponseEntity<?> getDream9Details(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear) {
        Optional<User> user = findGuestByToken(bear);
        if(!user.isPresent()) return ResponseEntity.ok(false);
        return ResponseEntity.ok(pointsRepository.findByUsername(user.get().getUsername()));
    }

    @GetMapping("/findAllRankForPlayers")
    public ResponseEntity<?> findAllRankForPlayers(@RequestHeader(HttpHeaders.AUTHORIZATION) String bear) {
        Optional<User> user = findGuestByToken(bear);
        if(!user.isPresent()) return ResponseEntity.ok(false);
        return ResponseEntity.ok(pointsRepository.findAllRankForPlayers(user.get().getMatchNumber()));
    }
    @GetMapping("/findAllRankByMatch")
    public ResponseEntity<?> findAllRankByMatch(@RequestParam(name = "matchNo") String matchNo) {
        return ResponseEntity.ok(pointsRepository.findAllRankForPlayers(matchNo));
    }

    @PostMapping("/createNewMatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createNewMatch(@RequestBody Tournament tournament) {
        Tournament res = tournamentRepository.save(tournament);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/updateInningsSession")
    public ResponseEntity<?> updateInningsSession(@RequestBody InningsSession match) {
        System.out.println(match.toString());
        List<BattingSession> battingSession = match.getBattingSession();
        List<BowlingSession> bowlingSession = match.getBowlingSession();
        Map<String,Integer> catchStump = new HashMap<>();
        battingSession.forEach(o->{
            if(Objects.nonNull(o.getCatchOrStumpedBy())){
                Integer temp = catchStump.get(lookUpMaker(match, o.getCatchOrStumpedBy()));
                catchStump.put(lookUpMaker(match,o.getCatchOrStumpedBy()) , temp!=null ? temp+1 : 1);
            }
        });
        battingSession.forEach(o->{
            Player batter = o.getBatterName();
            Player catchOrStumpBy = o.getCatchOrStumpedBy();
            ScoringDB batterData = scoringDbRepository.findScoringDBByLookup(lookUpMaker(match,batter));
            ScoringDB catchOrStumpByData = scoringDbRepository.findScoringDBByLookup(lookUpMaker(match,catchOrStumpBy));
            if(Objects.nonNull(batter) && Objects.isNull(batterData)){
                scoringDbRepository.save(ScoringDB.builder()
                        .lookup(lookUpMaker(match,batter))
                        .playerName(batter.getName())
                        .playerId(batter.getId())
                        .matchNo(match.getMatch())
                        .batting(true)
                        .bowling(false)
                        .runs(o.getRuns())
                        .ballsFaced(o.getBalls())
                        .fours(o.getFours())
                        .sixes(o.getSixes())
                        .duck(o.getRuns()==0 && o.isOut())
                        .build());
            }else if(Objects.nonNull(batterData)){
                batterData.setBatting(true);
                batterData.setRuns(o.getRuns());
                batterData.setBallsFaced(o.getBalls());
                batterData.setFours(o.getFours());
                batterData.setSixes(o.getSixes());
                batterData.setDuck(o.getRuns()==0 && o.isOut());
                scoringDbRepository.save(batterData);
            }
            if(Objects.nonNull(catchOrStumpBy) && Objects.isNull(catchOrStumpByData)){
                scoringDbRepository.save(ScoringDB.builder()
                        .lookup(lookUpMaker(match,catchOrStumpBy))
                        .playerName(catchOrStumpBy.getName())
                        .playerId(catchOrStumpBy.getId())
                        .matchNo(match.getMatch())
                        .catchOrStumps(catchStump.get(lookUpMaker(match,catchOrStumpBy)))
                        .build());
            }else if(Objects.nonNull(catchOrStumpByData)){
                catchOrStumpByData.setCatchOrStumps(catchStump.get(lookUpMaker(match,catchOrStumpBy)));
                scoringDbRepository.save(catchOrStumpByData);
            }

        });
        bowlingSession.forEach(o->{
            Player bowler = o.getBowlerName();
            ScoringDB bowlerData = scoringDbRepository.findScoringDBByLookup(lookUpMaker(match,bowler));
            if(Objects.nonNull(bowler) && Objects.isNull(bowlerData)){
                scoringDbRepository.save(ScoringDB.builder()
                        .lookup(lookUpMaker(match,bowler))
                        .playerName(bowler.getName())
                        .playerId(bowler.getId())
                        .matchNo(match.getMatch())
                        .batting(false)
                        .bowling(true)
                        .overs(o.getOvers())
                        .dots(o.getDots())
                        .runsConceded(o.getRuns())
                        .wickets(o.getWickets())
                        .build());
            }else if(Objects.nonNull(bowlerData)){
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
    public ResponseEntity<?> findSubstitutionByUsername(@RequestParam(name = "username") String username,@RequestParam(name = "matchNo") String matchNo) {
        return ResponseEntity.ok(substitutionRepository.findById(pointsLookup(matchNo,username)));
    }
    private String lookUpMaker(InningsSession match,Player a){
        if(Objects.isNull(a) || Objects.isNull(a.getId())) {
            System.out.println(a);
            return "";
        }
        return match.getMatch().trim().concat("_").concat(Long.toString(a.getId()));
    }

    private void pointsDBProcessScore(String matchNo){
        List<Points> pointsDatas = pointsRepository.findPointsByMatchNo(matchNo);
        List<ScoringDB> matchScoreDBs = scoringDbRepository.findScoringDBSByMatchNo(matchNo);
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> batMap = new HashMap<>();
        Map<String, Integer> bowlMap = new HashMap<>();
        for (ScoringDB score : matchScoreDBs) {
            map.put(score.getLookup(), score.getTotalPlayerPoint());
            batMap.put(score.getLookup(),score.getBattingPoints());
            bowlMap.put(score.getLookup(),score.getBowlingPoints());
        }
        for (Points p : pointsDatas) {
            double total =0;
            if(map.get(lookup2(matchNo, p.getCaptain())) !=null){
                p.setCaptainPoint(map.get(lookup2(matchNo, p.getCaptain())) * 2.0);
                total = total + p.getCaptainPoint();
            }
            if(map.get(lookup2(matchNo, p.getVcaptain())) !=null){
                p.setVcaptainPoint(map.get(lookup2(matchNo, p.getVcaptain())) * 1.5);
                total = total + p.getVcaptainPoint();
            }
            if(batMap.get(lookup2(matchNo, p.getBattinghero())) !=null){
                p.setBattingheroPoint(map.get(lookup2(matchNo, p.getBattinghero())) * 1.0 + batMap.get(lookup2(matchNo, p.getBattinghero()))*0.5);
                total = total + p.getBattingheroPoint();
            }
            if(bowlMap.get(lookup2(matchNo, p.getBowlinghero())) !=null){
                p.setBowlingheroPoint(map.get(lookup2(matchNo, p.getBowlinghero())) * 1.0 + bowlMap.get(lookup2(matchNo, p.getBowlinghero())) * 0.5);
                total = total + p.getBowlingheroPoint();
            }
            if(map.get(lookup2(matchNo, p.getPlayer5())) !=null){
                p.setPlayer5Point(map.get(lookup2(matchNo, p.getPlayer5())) * 1.0);
                total = total + p.getPlayer5Point();
            }
            if(map.get(lookup2(matchNo, p.getPlayer6())) !=null){
                p.setPlayer6Point(map.get(lookup2(matchNo, p.getPlayer6())) * 1.0);
                total = total + p.getPlayer6Point();
            }
            if(map.get(lookup2(matchNo, p.getPlayer7())) !=null){
                p.setPlayer7Point(map.get(lookup2(matchNo, p.getPlayer7())) * 1.0);
                total = total + p.getPlayer7Point();
            }
            if(map.get(lookup2(matchNo, p.getPlayer8())) !=null){
                p.setPlayer8Point(map.get(lookup2(matchNo, p.getPlayer8())) * 1.0);
                total = total + p.getPlayer8Point();
            }
            if(map.get(lookup2(matchNo, p.getPlayer9())) !=null){
                p.setPlayer9Point(map.get(lookup2(matchNo, p.getPlayer9())) * 1.0);
                total = total + p.getPlayer9Point();
            }

            p.setTotal(total);
        }
        pointsDatas.sort(Collections.reverseOrder());
        int rank=0;
        for (Points p : pointsDatas){
            p.setRank_no(++rank);
        }
        pointsRepository.saveAll(pointsDatas);
    }
    private String lookup2(String matchNo,Long playerID ){
        return matchNo.trim().trim().concat("_").concat(Long.toString(playerID));
    }

    private String pointsLookup(String matchNo,String username ){
        return matchNo.trim().trim().concat("_").concat(username);
    }

    private void scoreDBFlagProcessor(String matchNo){
        List<ScoringDB> matchScoreDB = scoringDbRepository.findScoringDBSByMatchNo(matchNo);
        matchScoreDB.forEach(o->{
            //score more than 30
            if(Objects.nonNull(o.getRuns()) && o.getRuns()>=30){
                o.setScore30(1);
            }
            //score more than 50
            if(Objects.nonNull(o.getRuns()) && o.getRuns()>=50){
                o.setScore50(1);
            }
            //score more than 100
            if(Objects.nonNull(o.getRuns()) && o.getRuns()>=100){
                o.setScore100(1);
            }
            //score more than 150
            if(Objects.nonNull(o.getRuns()) && o.getRuns()>=150){
                o.setScore150(1);
            }
            //3 wickets
            if(Objects.nonNull(o.getWickets()) && o.getWickets()>=3){
                o.setWicket3(1);
            }
            //4 wickets
            if(Objects.nonNull(o.getWickets()) && o.getWickets()>=4){
                o.setWicket4(1);
            }
            //5 wickets
            if(Objects.nonNull(o.getWickets()) && o.getWickets()>=5){
                o.setWicket5(1);
            }
            //6 wickets
            if(Objects.nonNull(o.getWickets()) && o.getWickets()>=6){
                o.setWicket6(1);
            }
            //Strike Rate
            if(Objects.nonNull(o.getRuns()) && Objects.nonNull(o.getBallsFaced()) && o.getBallsFaced()>=6){
                o.setStrikeRate(strikerate(o.getBallsFaced(),o.getRuns()));
            }
            // Economy
            if(Objects.nonNull(o.getRunsConceded()) && Objects.nonNull(o.getOvers()) && o.getOvers()>=1.0){
                o.setEconomy(economy(o.getOvers(),o.getRunsConceded()));
            }
            //---------------Batter-------------------
            //is run scored ?
            if(Objects.nonNull(o.getRuns()) && o.getRuns()>0){
                o.setRunScoreBonus(o.getRuns());
            }
            //4s & 6s bonus
            if(Objects.nonNull(o.getFours()) || Objects.nonNull(o.getSixes())){
                int bonus = Objects.nonNull(o.getFours()) ? o.getFours()*2 : 0;
                bonus = bonus + (Objects.nonNull(o.getSixes()) ? o.getSixes()*4 : 0);
                o.setFoursAndSixesBonus(bonus);
            }
            //30,50,100,150 runs
            if(Objects.nonNull(o.getScore30()) || Objects.nonNull(o.getScore50()) || Objects.nonNull(o.getScore100()) || Objects.nonNull(o.getScore150()) ){
                int bonus = Objects.nonNull(o.getScore30()) ? o.getScore30() * 5 : 0;
                bonus = bonus + (Objects.nonNull(o.getScore50()) ? o.getScore50()*10 : 0);
                bonus = bonus + (Objects.nonNull(o.getScore100()) ? o.getScore100()*50 : 0);
                bonus = bonus + (Objects.nonNull(o.getScore150()) ? o.getScore150()*100 : 0);
                o.setBonusFor30_50_100_150(bonus);
            }
            //Duck Bonus
            if(Objects.nonNull(o.getDuck())){
                int bonus = o.getDuck() ? -5 : 0;
                o.setDuckBonus(bonus);
            }
            //SR bonus
            if(Objects.nonNull(o.getStrikeRate()) && o.getStrikeRate()>0){
                double P2 = o.getStrikeRate();
                int bonus = 0;
                if(P2>170){
                    bonus = 6;
                }else if(P2>150){
                    bonus = 4;
                }else if(P2>=130){
                    bonus = 2;
                }else if(P2>=60 && P2<=70){
                    bonus = -2;
                }else if(P2>=50 && P2<60){
                    bonus = -4;
                }
                else if(P2<50){
                    bonus = -6;
                }
                o.setSRBonus(bonus);
            }
            //batting bonus - batting hero
            o.setBattingPoints(
                    (o.getRunScoreBonus()!=null ? o.getRunScoreBonus() : 0) +
                    (o.getFoursAndSixesBonus()!=null ? o.getFoursAndSixesBonus() : 0)+
                    (o.getBonusFor30_50_100_150() != null ? o.getBonusFor30_50_100_150():0)+
                    (o.getDuckBonus()!=null ? o.getDuckBonus() : 0)
                    +(o.getSRBonus()!=null ? o.getSRBonus() : 0)
            );

            //--------------Bowler--------------
            //Wkts Bonus
            if(Objects.nonNull(o.getWickets()) && o.getWickets()>0){
                o.setWicketBonus(o.getWickets()*25);
            }
            //Dot Bonus
            if(Objects.nonNull(o.getDots()) && o.getDots()>0){
                int bonus = o.getDots() * 6;
                o.setDotBonus(bonus);
            }
            //3 wkts,4wkts,5wkts,6wkts bonus
            if(Objects.nonNull(o.getWicket3()) || Objects.nonNull(o.getWicket4()) || Objects.nonNull(o.getWicket5()) || Objects.nonNull(o.getWicket6()) ){
                int bonus = Objects.nonNull(o.getWicket3()) ? o.getWicket3() * 12 : 0;
                bonus = bonus + (Objects.nonNull(o.getWicket4()) ? o.getWicket4()*16 : 0);
                bonus = bonus + (Objects.nonNull(o.getWicket5()) ? o.getWicket5()*20 : 0);
                bonus = bonus + (Objects.nonNull(o.getWicket6()) ? o.getWicket6()*30 : 0);
                o.setBonusFor3wk4wk5wk6wk(bonus);
            }
            //Catch Bonus
            if(Objects.nonNull(o.getCatchOrStumps()) && o.getCatchOrStumps()>0){
                o.setCatchBonus(o.getCatchOrStumps()*4);
            }

            //Economy bonus
            if(Objects.nonNull(o.getEconomy()) && o.getEconomy()>0){
               double U2 = o.getEconomy();
                int bonus = 0;
                if(U2<=5){
                    bonus = 6;
                } else if(U2<=7){
                    bonus = 4;
                }else if(U2>=10 && U2<=12){
                    bonus = -4;
                }else if(U2>12){
                    bonus = -6;
                }
                o.setEconomyBonus(bonus);
            }
            //bowling points
            o.setBowlingPoints(
                    (o.getWicketBonus() != null ? o.getWicketBonus() : 0)+
                            (o.getDotBonus() !=null ? o.getDotBonus() : 0)+
                            (o.getBonusFor3wk4wk5wk6wk() !=null ? o.getBonusFor3wk4wk5wk6wk() : 0)+
                            (o.getEconomyBonus() !=null ? o.getEconomyBonus() : 0)
            );
            //total score
            int totalScrore =0;
            if(Objects.nonNull(o.getRunScoreBonus()) && o.getRunScoreBonus()>0){
                totalScrore = totalScrore +o.getRunScoreBonus();
            }
            if(Objects.nonNull(o.getFoursAndSixesBonus()) && o.getFoursAndSixesBonus()>0){
                totalScrore = totalScrore +o.getFoursAndSixesBonus();
            }
            if(Objects.nonNull(o.getBonusFor30_50_100_150()) && o.getBonusFor30_50_100_150()>0){
                totalScrore = totalScrore +o.getBonusFor30_50_100_150();
            }
            if(Objects.nonNull(o.getSRBonus()) && o.getSRBonus()>0){
                totalScrore = totalScrore +o.getSRBonus();
            }
            if(Objects.nonNull(o.getDuckBonus()) && o.getDuckBonus()>0){
                totalScrore = totalScrore +o.getDuckBonus();
            }
            if(Objects.nonNull(o.getWicketBonus()) && o.getWicketBonus()>0){
                totalScrore = totalScrore +o.getWicketBonus();
            }
            if(Objects.nonNull(o.getDotBonus()) && o.getDotBonus()>0){
                totalScrore = totalScrore +o.getDotBonus();
            }
            if(Objects.nonNull(o.getBonusFor3wk4wk5wk6wk()) && o.getBonusFor3wk4wk5wk6wk()>0){
                totalScrore = totalScrore +o.getBonusFor3wk4wk5wk6wk();
            }
            if(Objects.nonNull(o.getEconomyBonus()) && o.getEconomyBonus()>0){
                totalScrore = totalScrore +o.getEconomyBonus();
            }
            if(Objects.nonNull(o.getCatchBonus()) && o.getCatchBonus()>0){
                totalScrore = totalScrore +o.getCatchBonus();
            }

            o.setTotalPlayerPoint(totalScrore);
        });

       scoringDbRepository.saveAll(matchScoreDB);
    }

    private double strikerate(Integer ballsFaced, Integer runs)
    {
         double ab= (double) runs/ballsFaced;
         return ab*100;
    }

    private Double economy(Double overs, Integer runsCon)
    {
        if(Objects.isNull(overs) || overs<1) return 0.0D;
        String[] arr = String.valueOf(overs).split("\\.");
        if(arr.length ==2){
            int fullOver = Integer.parseInt(arr[0]);
            int balls = Integer.parseInt(arr[1]);
            Double normalizedOver = (double) ((fullOver * 6 + balls) / 6);
            return runsCon/normalizedOver;
        }else return runsCon/overs ;
    }

    public Optional<User> findGuestByToken(String bear){
        return getGuests(bear, jwtProvider);
    }

    private Optional<User> getGuests(String bear, JwtProvider jwtProvider) {
        if(bear.equals("")) return Optional.empty();
        String token = bear.replaceAll("Bearer ","").trim();
        String user = jwtProvider.getUserNameFromJwtToken(token);
        return userRepository.findByUsername(user);
    }

}
