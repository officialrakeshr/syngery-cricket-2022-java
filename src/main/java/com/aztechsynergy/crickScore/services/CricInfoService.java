package com.aztechsynergy.crickScore.services;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name="cricinfo", url = "https://hs-consumer-api.espncricinfo.com/v1")
public interface CricInfoService {


    @GetMapping ("/pages/match/scorecard")
    Map<String,Object> getIPLMatchScoreCard(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer matchId);
    @GetMapping ("/pages/match/details")
    Map<String,Object> getIPLMatchDetails(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer matchId, @RequestParam Boolean latest);
    @GetMapping ("/pages/match/overs/details")
    Map<String,Object> getIPLMatchOverDetails(@RequestParam String lang, @RequestParam Integer seriesId, @RequestParam Integer matchId, @RequestParam String mode);
}
