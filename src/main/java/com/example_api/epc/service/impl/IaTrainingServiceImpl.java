package com.example_api.epc.service.impl;

import com.example_api.epc.service.IaTrainingService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Service
public class IaTrainingServiceImpl implements IaTrainingService {


    private static final String PYTHON =
                    "/home/eduardo-pinheiro/Documents/epc-ia-bet/epc-ia/venv/bin/python";

    private static final File WORKDIR =
                    new File("/home/eduardo-pinheiro/Documents/epc-ia-bet/epc-ia");

    @Async
    @Override
    public void retrain() {
        runJob("src.jobs.populate_market_performance");
        runJob("src.jobs.populate_team_strength");
    }

    private void runJob(String module) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                            PYTHON, "-m", module
            );
            pb.directory(WORKDIR);
            pb.redirectErrorStream(true);

            Process p = pb.start();

            BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getInputStream())
            );

            String line;
            while ((line = r.readLine()) != null) {
                System.out.println("[IA] " + line);
            }

            p.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
