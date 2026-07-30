package com.example_api.epc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpreadsheetImportResponse {

    private boolean valid;
    private boolean imported;

    private int fixturesRead;
    private int statisticsRead;
    private int sourcesRead;

    private int fixturesInserted;
    private int fixturesUpdated;
    private int leaguesInserted;
    private int teamsInserted;
    private int statisticsUpserted;
    private int sourcesInserted;

    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    @Builder.Default
    private List<FixturePreview> fixtures = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        private String severity;
        private String sheet;
        private Integer row;
        private String field;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixturePreview {
        private String fixtureRef;
        private String action;
        private String leagueName;
        private String leagueCountry;
        private OffsetDateTime kickoffAt;
        private String status;
        private String homeTeamName;
        private String awayTeamName;
        private Integer homeGoals;
        private Integer awayGoals;
        private boolean homeStatistics;
        private boolean awayStatistics;
        private int sourceCount;
    }
}
