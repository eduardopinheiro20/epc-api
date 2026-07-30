package com.example_api.epc.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "ticket_selections")
public class TicketSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", insertable = false, updatable = false)
    @JsonBackReference
    private Ticket ticket;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "fixture_id")
    private Long fixtureId;

    private String market;

    @Column(name = "market_code")
    private String marketCode;

    private Double odd;

    @Column(name = "odd_source")
    private String oddSource;

    @Column(name = "odd_captured_at")
    private OffsetDateTime oddCapturedAt;

    private Double prob;

    @Column(name = "home_name")
    private String homeName;

    @Column(name = "away_name")
    private String awayName;

}
