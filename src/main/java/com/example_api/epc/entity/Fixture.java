package com.example_api.epc.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

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

    @Column(name = "api_id", unique = true)
    private Integer apiId;

    @Column(name = "league_id")
    private Long leagueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", insertable = false, updatable = false)
    private League league;

    private OffsetDateTime date;

    @Column(name = "home_team_id")
    private Long homeTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", insertable = false, updatable = false)
    private Team homeTeam;

    @Column(name = "away_team_id")
    private Long awayTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", insertable = false, updatable = false)
    private Team awayTeam;

    private String status;

    @Column(name = "home_goals")
    private Integer homeGoals;

    @Column(name = "away_goals")
    private Integer awayGoals;

    // Estatísticas geradas pela IA / Collector
    @Column(name = "home_avg_scored")
    private Double homeAvgScored;

    @Column(name = "home_avg_conceded")
    private Double homeAvgConceded;

    @Column(name = "home_recent_for", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> homeRecentFor;

    @Column(name = "home_recent_against", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> homeRecentAgainst;

    @Column(name = "away_avg_scored")
    private Double awayAvgScored;

    @Column(name = "away_avg_conceded")
    private Double awayAvgConceded;

    @Column(name = "away_recent_for", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> awayRecentFor;

    @Column(name = "away_recent_against", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> awayRecentAgainst;

    @Column(name = "home_shots_total")
    private Integer homeShotsTotal;

    @Column(name = "away_shots_total")
    private Integer awayShotsTotal;

    @Column(name = "home_shots_on_target")
    private Integer homeShotsOnTarget;

    @Column(name = "away_shots_on_target")
    private Integer awayShotsOnTarget;

    @Column(name = "home_corners")
    private Integer homeCorners;

    @Column(name = "away_corners")
    private Integer awayCorners;

    @Column(name = "home_yellow")
    private Integer homeYellow;

    @Column(name = "away_yellow")
    private Integer awayYellow;

    @Column(name = "home_red")
    private Integer homeRed;

    @Column(name = "away_red")
    private Integer awayRed;

    @Column(name = "home_possession")
    private Double homePossession;

    @Column(name = "away_possession")
    private Double awayPossession;

    @Column(name = "home_pass_accuracy")
    private Double homePassAccuracy;

    @Column(name = "away_pass_accuracy")
    private Double awayPassAccuracy;

    @Column(name = "home_assists")
    private Integer homeAssists;

    @Column(name = "away_assists")
    private Integer awayAssists;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
