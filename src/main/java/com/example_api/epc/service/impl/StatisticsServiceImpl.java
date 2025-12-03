package com.example_api.epc.service.impl;

import com.example_api.epc.entity.Fixture;
import com.example_api.epc.entity.MatchStatistics;
import com.example_api.epc.entity.Team;
import com.example_api.epc.repository.FixtureRepository;
import com.example_api.epc.repository.MatchStatisticsRepository;
import com.example_api.epc.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final FixtureRepository fixtureRepo;
    private final MatchStatisticsRepository statsRepo;

    @Override
    public List<Map<String, Object>> getAllFixtureStatistics() {

        List<Fixture> fixtures = fixtureRepo.findByStatus("FT");

        List<Map<String, Object>> result = new ArrayList<>();

        for (Fixture f : fixtures) {

            Map<String, Object> item = new HashMap<>();
            item.put("fixtureId", f.getId());
            item.put("league", f.getLeague().getName());
            item.put("date", f.getDate());

            // HOME
            Team home = f.getHomeTeam();
            MatchStatistics homeStats = statsRepo.findByFixtureIdAndTeamId(
                            f.getId(),
                            home.getId()
            );

            Map<String, Object> homeMap = new HashMap<>();
            homeMap.put("teamId", home.getId());
            homeMap.put("teamName", home.getName());
            homeMap.put("stats", homeStats);

            // AWAY
            Team away = f.getAwayTeam();
            MatchStatistics awayStats = statsRepo.findByFixtureIdAndTeamId(
                            f.getId(),
                            away.getId()
            );

            Map<String, Object> awayMap = new HashMap<>();
            awayMap.put("teamId", away.getId());
            awayMap.put("teamName", away.getName());
            awayMap.put("stats", awayStats);

            item.put("home", homeMap);
            item.put("away", awayMap);

            result.add(item);
        }

        return result;
    }
}
