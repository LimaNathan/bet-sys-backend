package com.coticbet.dto.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OddsApiEventResponse {

    private String id;

    @JsonProperty("sport_key")
    private String sportKey;

    @JsonProperty("sport_title")
    private String sportTitle;

    @JsonProperty("commence_time")
    private String commenceTime;

    @JsonProperty("home_team")
    private String homeTeam;

    @JsonProperty("away_team")
    private String awayTeam;

    private List<Bookmaker> bookmakers;

    @Data
    public static class Bookmaker {
        private String key;
        private String title;
        private List<Market> markets;
    }

    @Data
    public static class Market {
        private String key;
        private List<Outcome> outcomes;
    }

    @Data
    public static class Outcome {
        private String name;
        private Double price;
    }
}
