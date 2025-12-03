package com.example_api.epc.repository;

import com.example_api.epc.entity.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {
    Optional<Fixture> findById(Long id);

    List<Fixture> findByStatus(String status);
}
