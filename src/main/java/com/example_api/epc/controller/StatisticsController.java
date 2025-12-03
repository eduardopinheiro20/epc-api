package com.example_api.epc.controller;

import com.example_api.epc.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService service;


    @GetMapping("/fixtures")
    public ResponseEntity<List<Map<String, Object>>> getAllStatistics() {
        return ResponseEntity.ok(service.getAllFixtureStatistics());
    }
}