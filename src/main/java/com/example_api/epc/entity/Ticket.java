package com.example_api.epc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    // para simplicidade, armazenamos a FK como coluna; mapeamento ManyToOne abaixo
    @Column(name = "bankroll_id", insertable = false, updatable = false)
    private Long bankrollId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bankroll_id")
    private com.example_api.epc.entity.Bankroll bankroll;

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

    // ---------- getters / setters ----------
    // (gerar todos os getters/setters ou usar Lombok @Data se preferir)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Double getFinalOdd() { return finalOdd; }
    public void setFinalOdd(Double finalOdd) { this.finalOdd = finalOdd; }

    public Double getCombinedProb() { return combinedProb; }
    public void setCombinedProb(Double combinedProb) { this.combinedProb = combinedProb; }

    public Boolean getAppliedToBankroll() { return appliedToBankroll; }
    public void setAppliedToBankroll(Boolean appliedToBankroll) { this.appliedToBankroll = appliedToBankroll; }

    public Long getBankrollId() { return bankrollId; }
    public void setBankrollId(Long bankrollId) { this.bankrollId = bankrollId; }

    public com.example_api.epc.entity.Bankroll getBankroll() { return bankroll; }
    public void setBankroll(com.example_api.epc.entity.Bankroll bankroll) { this.bankroll = bankroll; }

    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
