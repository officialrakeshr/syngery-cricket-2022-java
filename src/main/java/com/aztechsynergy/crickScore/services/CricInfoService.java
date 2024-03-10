package com.aztechsynergy.crickScore.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name="cricinfo", url = "https://hs-consumer-api.espncricinfo.com/v1")
public interface CricInfoService {

    @GetMapping ("/pages/series/teams")
    Map<String,Object> getIPLTeams(@RequestParam String lang, @RequestParam Integer seriesId);
    @GetMapping ("/pages/series/schedule")
    Map<String,Object> getIPLSchedule(@RequestParam String lang, @RequestParam Integer seriesId);
    @GetMapping ("/pages/series/squads")
    Map<String,Object> getIPLSquads(@RequestParam String lang, @RequestParam Integer seriesId);
    @GetMapping ("/pages/series/squad/details")
    Map<String,Object> getIPLSquadDetailsBySquadId(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer squadId);
    @GetMapping ("/pages/match/scorecard")
    Map<String,Object> getIPLMatchScoreCard(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer matchId);
    @GetMapping ("/pages/match/details")
    Map<String,Object> getIPLMatchDetails(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer matchId, @RequestParam Boolean latest);
    @GetMapping ("/pages/match/overs/details")
    Map<String,Object> getIPLMatchOverDetails(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer matchId, @RequestParam String mode);
}
