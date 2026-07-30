package com.example_api.epc.repository;

import com.example_api.epc.entity.StatsLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatsLogRepository extends JpaRepository<StatsLog, Long> {

    boolean existsByFixtureIdAndEndpointAndMessage(Long fixtureId, String endpoint, String message);
}
