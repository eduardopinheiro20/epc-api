package com.example_api.epc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "match_statistics")
public class MatchStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixture_id")
    private Long fixtureId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "shots_total")
    private Integer shotsTotal;

    @Column(name = "shots_on_goal")
    private Integer shotsOnGoal;

    @Column(name = "shots_off_goal")
    private Integer shotsOffGoal;

    @Column(name = "blocked_shots")
    private Integer blockedShots;

    @Column(name = "shots_inside_box")
    private Integer shotsInsideBox;

    @Column(name = "shots_outside_box")
    private Integer shotsOutsideBox;

    @Column(name = "possession")
    private Float possession;

    @Column(name = "corners")
    private Integer corners;

    @Column(name = "fouls")
    private Integer fouls;

    @Column(name = "yellow_cards")
    private Integer yellowCards;

    @Column(name = "red_cards")
    private Integer redCards;

    @Column(name = "saves")
    private Integer saves;

    @Column(name = "total_passes")
    private Integer totalPasses;

    @Column(name = "accurate_passes")
    private Integer accuratePasses;

    @Column(name = "pass_accuracy")
    private Float passAccuracy;

    @Column(name = "expected_goals")
    private Float expectedGoals;

    @Column(name = "dangerous_attacks")
    private Integer dangerousAttacks;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}
