package com.coticbet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coticbet.dto.response.LeaderboardEntry;
import com.coticbet.service.LeaderboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(
            @RequestParam(defaultValue = "wealth") String type) {

        List<LeaderboardEntry> entries = switch (type.toLowerCase()) {
            case "profit" -> leaderboardService.getProfitRanking();
            case "loss" -> leaderboardService.getLossRanking();
            default -> leaderboardService.getWealthRanking();
        };

        return ResponseEntity.ok(entries);
    }
}
