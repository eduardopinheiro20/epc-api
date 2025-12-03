package com.example_api.epc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // quem criou (opcional)
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "saved_at", updatable = false)
    private LocalDateTime savedAt;

    @Column(name = "status")
    private String status; // PENDING, FINISHED

    @Column(name = "result")
    private String result; // GREEN, RED, UNKNOWN

    @Column(name = "final_odd", nullable = false)
    private Double finalOdd;

    @Column(name = "combined_prob")
    private Double combinedProb;

    @Column(name = "applied_to_bankroll")
    private Boolean appliedToBankroll = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bankroll_id")
    private Bankroll bankroll;

    // meta como JSON serializado (string)
    @Column(name = "meta", columnDefinition = "text")
    private String meta;

    @Column(name = "signature", unique = true)
    private String signature;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---------- lifecycle callbacks ----------
    @PrePersist
    public void prePersist() {
        if (savedAt == null) savedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
