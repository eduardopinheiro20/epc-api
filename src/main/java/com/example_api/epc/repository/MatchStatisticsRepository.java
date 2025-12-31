package com.example_api.epc.repository;

import com.example_api.epc.entity.MatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {
    MatchStatistics findByFixtureIdAndTeamId(Long fixtureId, Long teamId);
}
