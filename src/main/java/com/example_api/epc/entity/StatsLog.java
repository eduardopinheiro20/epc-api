package com.example_api.epc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "stats_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixture_id")
    private Long fixtureId;

    @Column(name = "team_id")
    private Long teamId;

    private String endpoint;

    private String status;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
