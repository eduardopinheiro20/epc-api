package com.example_api.epc.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ticket_selections")
public class TicketSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "fixture_id")
    private Long fixtureId;

    @Column(name = "market")
    private String market;

    @Column(name = "odd")
    private Double odd;

    @Column(name = "prob")
    private Double prob;

    @Column(name = "home_name")
    private String homeName;

    @Column(name = "away_name")
    private String awayName;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }

    public Double getOdd() { return odd; }
    public void setOdd(Double odd) { this.odd = odd; }

    public Double getProb() { return prob; }
    public void setProb(Double prob) { this.prob = prob; }

    public String getHomeName() { return homeName; }
    public void setHomeName(String homeName) { this.homeName = homeName; }

    public String getAwayName() { return awayName; }
    public void setAwayName(String awayName) { this.awayName = awayName; }
}