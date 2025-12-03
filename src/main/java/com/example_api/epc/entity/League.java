package com.example_api.epc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "leagues")
@Getter
@Setter
@NoArgsConstructor
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", unique = true)
    private Integer apiId;

    private String name;

    private String country;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


    @OneToMany(mappedBy = "league", fetch = FetchType.LAZY)
    private List<Fixture> fixtures;
}