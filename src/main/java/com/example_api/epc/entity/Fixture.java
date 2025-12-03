package com.example_api.epc.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fixtures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fixture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id")
    private Integer apiId;

//    @Column(name = "league_id")
//    private Integer leagueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", insertable = false, updatable = false)
    private League league;

    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", insertable = false, updatable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", insertable = false, updatable = false)
    private Team awayTeam;

//    @Column(name = "home_team_id")
//    private Integer homeTeamId;
//
//    @Column(name = "away_team_id")
//    private Integer awayTeamId;

    private String status;

    @Column(name = "home_goals")
    private Integer homeGoals;

    @Column(name = "away_goals")
    private Integer awayGoals;

    // Estatísticas geradas pela IA / Collector
    private Double homeAvgScored;
    private Double homeAvgConceded;

    @Column(columnDefinition = "jsonb")
    private String homeRecentFor;

    @Column(columnDefinition = "jsonb")
    private String homeRecentAgainst;

    private Double awayAvgScored;
    private Double awayAvgConceded;

    @Column(columnDefinition = "jsonb")
    private String awayRecentFor;

    @Column(columnDefinition = "jsonb")
    private String awayRecentAgainst;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
