package com.example_api.epc.repository;

import com.example_api.epc.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByApiId(Integer apiId);

    Optional<Team> findFirstByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);

    Optional<Team> findFirstByNameIgnoreCase(String name);

    List<Team> findAllByNameIgnoreCase(String name);
}
