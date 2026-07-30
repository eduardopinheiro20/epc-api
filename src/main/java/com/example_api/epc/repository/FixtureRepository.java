package com.example_api.epc.repository;

import com.example_api.epc.entity.Fixture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {
    Optional<Fixture> findById(Long id);

    Optional<Fixture> findByApiId(Integer apiId);

    Optional<Fixture> findFirstByLeagueIdAndHomeTeamIdAndAwayTeamIdAndDate(
                    Long leagueId,
                    Long homeTeamId,
                    Long awayTeamId,
                    OffsetDateTime date
    );

    List<Fixture> findByStatus(String status);

    @Query("""
                    select f from Fixture f
                    where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
                    order by f.date desc
                    """)
    List<Fixture> findAllByTeamId(@Param("teamId") Long teamId);

    @Query("""
                    select f from Fixture f
                    where (f.homeTeamId = :teamId or f.awayTeamId = :teamId)
                      and f.status in ('FT', 'AET', 'PEN')
                      and f.homeGoals is not null
                      and f.awayGoals is not null
                    order by f.date desc
                    """)
    List<Fixture> findCompletedByTeamId(@Param("teamId") Long teamId);
}
