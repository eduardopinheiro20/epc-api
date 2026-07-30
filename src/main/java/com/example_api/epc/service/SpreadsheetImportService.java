package com.example_api.epc.service;

import com.example_api.epc.dto.SpreadsheetImportResponse;
import com.example_api.epc.dto.SpreadsheetImportResponse.FixturePreview;
import com.example_api.epc.dto.SpreadsheetImportResponse.Issue;
import com.example_api.epc.entity.*;
import com.example_api.epc.exception.SpreadsheetImportException;
import com.example_api.epc.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.IntFunction;

@Service
@RequiredArgsConstructor
public class SpreadsheetImportService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final int MAX_DATA_ROWS = 5_000;

    private static final String PARTIDAS = "PARTIDAS";
    private static final String ESTATISTICAS = "ESTATISTICAS";
    private static final String FONTES = "FONTES";
    private static final String TIMES_CADASTRADOS = "TIMES_CADASTRADOS";
    private static final String LIGAS_CADASTRADAS = "LIGAS_CADASTRADAS";
    private static final String MODEL_RESOURCE =
                    "templates/modelo_importacao_epc_ia.xlsx";

    private static final Set<String> FINAL_STATUSES = Set.of("FT", "AET", "PEN");
    private static final Set<String> ALLOWED_STATUSES = Set.of(
                    "NS", "TBD", "FT", "AET", "PEN", "PST", "CANC",
                    "SUSP", "ABD", "1H", "HT", "2H", "ET", "LIVE"
    );

    private static final List<String> PARTIDAS_HEADERS = List.of(
                    "fixture_ref",
                    "fixture_api_id",
                    "league_api_id",
                    "league_name",
                    "league_country",
                    "kickoff_at",
                    "status",
                    "home_team_api_id",
                    "home_team_name",
                    "home_team_country",
                    "away_team_api_id",
                    "away_team_name",
                    "away_team_country",
                    "home_goals",
                    "away_goals"
    );

    private static final List<String> ESTATISTICAS_HEADERS = List.of(
                    "fixture_ref",
                    "team_side",
                    "shots_total",
                    "shots_on_goal",
                    "shots_off_goal",
                    "blocked_shots",
                    "shots_inside_box",
                    "shots_outside_box",
                    "possession",
                    "corners",
                    "fouls",
                    "yellow_cards",
                    "red_cards",
                    "saves",
                    "total_passes",
                    "accurate_passes",
                    "pass_accuracy",
                    "expected_goals",
                    "dangerous_attacks",
                    "assists"
    );

    private static final List<String> FONTES_HEADERS = List.of(
                    "fixture_ref",
                    "source_type",
                    "source_url",
                    "collected_at",
                    "confidence",
                    "notes"
    );

    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository statisticsRepository;
    private final StatsLogRepository statsLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public byte[] generateModel() {
        ClassPathResource resource = new ClassPathResource(MODEL_RESOURCE);
        try (
                        InputStream input = resource.getInputStream();
                        Workbook workbook = WorkbookFactory.create(input);
                        ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            List<Team> teams = new ArrayList<>(teamRepository.findAll());
            teams.sort(Comparator.comparing(
                            Team::getId,
                            Comparator.nullsLast(Long::compareTo)
            ));
            writeTeamsCatalog(workbook, teams);

            List<League> leagues = new ArrayList<>(leagueRepository.findAll());
            leagues.sort(Comparator.comparing(
                            League::getId,
                            Comparator.nullsLast(Long::compareTo)
            ));
            writeLeaguesCatalog(workbook, leagues);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(
                            "Não foi possível gerar o modelo de importação.",
                            ex
            );
        }
    }

    public SpreadsheetImportResponse validate(MultipartFile file) {
        ParsedWorkbook parsed = parse(file);
        populatePreview(parsed);
        return parsed.response();
    }

    @Transactional
    public SpreadsheetImportResponse importWorkbook(MultipartFile file) {
        ParsedWorkbook parsed = parse(file);
        populatePreview(parsed);

        if (hasErrors(parsed.response())) {
            throw new SpreadsheetImportException(
                            "A planilha possui erros e não foi importada.",
                            parsed.response()
            );
        }

        persist(parsed);
        parsed.response().setValid(true);
        parsed.response().setImported(true);
        return parsed.response();
    }

    private void writeTeamsCatalog(Workbook workbook, List<Team> teams) {
        Sheet sheet = recreateSheet(workbook, TIMES_CADASTRADOS);
        writeCatalogHeader(
                        workbook,
                        sheet,
                        List.of(
                                        "api_id_usar_na_planilha",
                                        "nome_exato",
                                        "pais",
                                        "id_interno_somente_consulta"
                        )
        );

        int rowIndex = 1;
        for (Team team : teams) {
            Row row = sheet.createRow(rowIndex++);
            writeNumber(row, 0, team.getApiId());
            writeText(row, 1, team.getName());
            writeText(row, 2, team.getCountry());
            writeNumber(row, 3, team.getId());
        }
        finishCatalogSheet(sheet, rowIndex, 4);
    }

    private void writeLeaguesCatalog(Workbook workbook, List<League> leagues) {
        Sheet sheet = recreateSheet(workbook, LIGAS_CADASTRADAS);
        writeCatalogHeader(
                        workbook,
                        sheet,
                        List.of(
                                        "league_api_id_usar_na_planilha",
                                        "nome_exato",
                                        "pais",
                                        "id_interno_somente_consulta"
                        )
        );

        int rowIndex = 1;
        for (League league : leagues) {
            Row row = sheet.createRow(rowIndex++);
            writeNumber(row, 0, league.getApiId());
            writeText(row, 1, league.getName());
            writeText(row, 2, league.getCountry());
            writeNumber(row, 3, league.getId());
        }
        finishCatalogSheet(sheet, rowIndex, 4);
    }

    private Sheet recreateSheet(Workbook workbook, String name) {
        int existingIndex = workbook.getSheetIndex(name);
        if (existingIndex >= 0) {
            workbook.removeSheetAt(existingIndex);
        }
        return workbook.createSheet(name);
    }

    private void writeCatalogHeader(
                    Workbook workbook,
                    Sheet sheet,
                    List<String> headers
    ) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        font.setColor(IndexedColors.WHITE.getIndex());

        Row header = sheet.createRow(0);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(style);
        }
    }

    private void writeText(Row row, int column, String value) {
        if (notBlank(value)) {
            row.createCell(column).setCellValue(value);
        }
    }

    private void writeNumber(Row row, int column, Number value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        }
    }

    private void finishCatalogSheet(Sheet sheet, int rowCount, int columnCount) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(
                        new CellRangeAddress(
                                        0,
                                        Math.max(0, rowCount - 1),
                                        0,
                                        columnCount - 1
                        )
        );
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
            sheet.setColumnWidth(
                            column,
                            Math.min(sheet.getColumnWidth(column) + 512, 60 * 256)
            );
        }
    }

    private ParsedWorkbook parse(MultipartFile file) {
        SpreadsheetImportResponse response = SpreadsheetImportResponse.builder().build();
        validateFile(file, response);

        if (hasErrors(response)) {
            return new ParsedWorkbook(new LinkedHashMap<>(), new ArrayList<>(), new ArrayList<>(), response);
        }

        Map<String, FixtureRow> fixtures = new LinkedHashMap<>();
        List<StatisticsRow> statistics = new ArrayList<>();
        List<SourceRow> sources = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            Sheet partidasSheet = requiredSheet(workbook, PARTIDAS, response);
            Sheet estatisticasSheet = requiredSheet(workbook, ESTATISTICAS, response);
            Sheet fontesSheet = requiredSheet(workbook, FONTES, response);

            if (hasErrors(response)) {
                return new ParsedWorkbook(fixtures, statistics, sources, response);
            }

            parseFixtures(partidasSheet, formatter, evaluator, fixtures, response);
            parseStatistics(estatisticasSheet, formatter, evaluator, statistics, response);
            parseSources(fontesSheet, formatter, evaluator, sources, response);
            validateRelationships(fixtures, statistics, sources, response);
        } catch (EncryptedDocumentException ex) {
            error(response, null, null, null, "A planilha está protegida por senha.");
        } catch (IOException | RuntimeException ex) {
            error(
                            response,
                            null,
                            null,
                            null,
                            "Não foi possível ler o arquivo .xlsx: " + safeMessage(ex)
            );
        }

        validateExistingIdentities(fixtures.values(), response);
        response.setFixturesRead(fixtures.size());
        response.setStatisticsRead(statistics.size());
        response.setSourcesRead(sources.size());
        response.setValid(!hasErrors(response));

        return new ParsedWorkbook(fixtures, statistics, sources, response);
    }

    private void validateFile(MultipartFile file, SpreadsheetImportResponse response) {
        if (file == null || file.isEmpty()) {
            error(response, null, null, "file", "Selecione uma planilha .xlsx.");
            return;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            error(response, null, null, "file", "O arquivo deve ter no máximo 10 MB.");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            error(response, null, null, "file", "Formato inválido. Envie um arquivo .xlsx.");
        }
    }

    private Sheet requiredSheet(
                    Workbook workbook,
                    String name,
                    SpreadsheetImportResponse response
    ) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            error(response, name, null, null, "Aba obrigatória ausente: " + name + ".");
        }
        return sheet;
    }

    private void parseFixtures(
                    Sheet sheet,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    Map<String, FixtureRow> fixtures,
                    SpreadsheetImportResponse response
    ) {
        Map<String, Integer> headers = readHeaders(
                        sheet,
                        PARTIDAS_HEADERS,
                        formatter,
                        evaluator,
                        response
        );
        if (headers.isEmpty()) {
            return;
        }

        validateRowLimit(sheet, PARTIDAS, response);

        int upperBound = Math.min(sheet.getLastRowNum(), MAX_DATA_ROWS);
        for (int rowIndex = 1; rowIndex <= upperBound; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlank(row, formatter, evaluator)) {
                continue;
            }

            int excelRow = rowIndex + 1;
            FixtureRow item = new FixtureRow();
            item.rowNumber = excelRow;
            item.fixtureRef = text(row, headers, "fixture_ref", formatter, evaluator);
            item.fixtureApiId = integer(row, headers, "fixture_api_id", formatter, evaluator, response, PARTIDAS, excelRow);
            item.leagueApiId = integer(row, headers, "league_api_id", formatter, evaluator, response, PARTIDAS, excelRow);
            item.leagueName = text(row, headers, "league_name", formatter, evaluator);
            item.leagueCountry = text(row, headers, "league_country", formatter, evaluator);
            item.kickoffAt = dateTime(row, headers, "kickoff_at", formatter, evaluator, response, PARTIDAS, excelRow);
            item.status = upper(text(row, headers, "status", formatter, evaluator));
            item.homeTeamApiId = integer(row, headers, "home_team_api_id", formatter, evaluator, response, PARTIDAS, excelRow);
            item.homeTeamName = text(row, headers, "home_team_name", formatter, evaluator);
            item.homeTeamCountry = text(row, headers, "home_team_country", formatter, evaluator);
            item.awayTeamApiId = integer(row, headers, "away_team_api_id", formatter, evaluator, response, PARTIDAS, excelRow);
            item.awayTeamName = text(row, headers, "away_team_name", formatter, evaluator);
            item.awayTeamCountry = text(row, headers, "away_team_country", formatter, evaluator);
            item.homeGoals = integer(row, headers, "home_goals", formatter, evaluator, response, PARTIDAS, excelRow);
            item.awayGoals = integer(row, headers, "away_goals", formatter, evaluator, response, PARTIDAS, excelRow);

            validateFixture(item, response);

            if (notBlank(item.fixtureRef)) {
                String key = normalizeKey(item.fixtureRef);
                if (fixtures.containsKey(key)) {
                    error(
                                    response,
                                    PARTIDAS,
                                    excelRow,
                                    "fixture_ref",
                                    "fixture_ref duplicado: " + item.fixtureRef + "."
                    );
                } else {
                    fixtures.put(key, item);
                }
            }
        }
    }

    private void validateFixture(FixtureRow item, SpreadsheetImportResponse response) {
        required(item.fixtureRef, PARTIDAS, item.rowNumber, "fixture_ref", response);
        required(item.leagueName, PARTIDAS, item.rowNumber, "league_name", response);
        required(item.leagueCountry, PARTIDAS, item.rowNumber, "league_country", response);
        required(item.homeTeamName, PARTIDAS, item.rowNumber, "home_team_name", response);
        required(item.homeTeamCountry, PARTIDAS, item.rowNumber, "home_team_country", response);
        required(item.awayTeamName, PARTIDAS, item.rowNumber, "away_team_name", response);
        required(item.awayTeamCountry, PARTIDAS, item.rowNumber, "away_team_country", response);
        required(item.status, PARTIDAS, item.rowNumber, "status", response);

        if (item.kickoffAt == null) {
            error(response, PARTIDAS, item.rowNumber, "kickoff_at", "Data/hora obrigatória no formato ISO 8601 com fuso.");
        }
        if (notBlank(item.status) && !ALLOWED_STATUSES.contains(item.status)) {
            error(
                            response,
                            PARTIDAS,
                            item.rowNumber,
                            "status",
                            "Status inválido. Use NS, FT, AET, PEN, PST, CANC, SUSP, ABD, TBD ou um status ao vivo."
            );
        }
        if (sameNormalized(item.homeTeamName, item.awayTeamName)) {
            error(
                            response,
                            PARTIDAS,
                            item.rowNumber,
                            "away_team_name",
                            "Mandante e visitante não podem ser o mesmo time."
            );
        }
        validateNonNegative(item.homeGoals, PARTIDAS, item.rowNumber, "home_goals", response);
        validateNonNegative(item.awayGoals, PARTIDAS, item.rowNumber, "away_goals", response);

        if (FINAL_STATUSES.contains(item.status)
                        && (item.homeGoals == null || item.awayGoals == null)) {
            error(
                            response,
                            PARTIDAS,
                            item.rowNumber,
                            "home_goals",
                            "Partidas encerradas precisam dos dois placares."
            );
        }
        if ("NS".equals(item.status)
                        && (item.homeGoals != null || item.awayGoals != null)) {
            warning(
                            response,
                            PARTIDAS,
                            item.rowNumber,
                            "status",
                            "Jogo NS possui placar; confira se o status deveria ser FT."
            );
        }
    }

    private void parseStatistics(
                    Sheet sheet,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    List<StatisticsRow> statistics,
                    SpreadsheetImportResponse response
    ) {
        Map<String, Integer> headers = readHeaders(
                        sheet,
                        ESTATISTICAS_HEADERS,
                        formatter,
                        evaluator,
                        response
        );
        if (headers.isEmpty()) {
            return;
        }

        validateRowLimit(sheet, ESTATISTICAS, response);
        Set<String> uniqueRows = new HashSet<>();
        int upperBound = Math.min(sheet.getLastRowNum(), MAX_DATA_ROWS);

        for (int rowIndex = 1; rowIndex <= upperBound; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlank(row, formatter, evaluator)) {
                continue;
            }

            int excelRow = rowIndex + 1;
            StatisticsRow item = new StatisticsRow();
            item.rowNumber = excelRow;
            item.fixtureRef = text(row, headers, "fixture_ref", formatter, evaluator);
            item.teamSide = upper(text(row, headers, "team_side", formatter, evaluator));
            item.shotsTotal = integer(row, headers, "shots_total", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.shotsOnGoal = integer(row, headers, "shots_on_goal", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.shotsOffGoal = integer(row, headers, "shots_off_goal", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.blockedShots = integer(row, headers, "blocked_shots", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.shotsInsideBox = integer(row, headers, "shots_inside_box", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.shotsOutsideBox = integer(row, headers, "shots_outside_box", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.possession = decimal(row, headers, "possession", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.corners = integer(row, headers, "corners", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.fouls = integer(row, headers, "fouls", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.yellowCards = integer(row, headers, "yellow_cards", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.redCards = integer(row, headers, "red_cards", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.saves = integer(row, headers, "saves", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.totalPasses = integer(row, headers, "total_passes", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.accuratePasses = integer(row, headers, "accurate_passes", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.passAccuracy = decimal(row, headers, "pass_accuracy", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.expectedGoals = decimal(row, headers, "expected_goals", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.dangerousAttacks = integer(row, headers, "dangerous_attacks", formatter, evaluator, response, ESTATISTICAS, excelRow);
            item.assists = integer(row, headers, "assists", formatter, evaluator, response, ESTATISTICAS, excelRow);

            validateStatistics(item, response);
            String uniqueKey = normalizeKey(item.fixtureRef) + "|" + item.teamSide;
            if (!uniqueRows.add(uniqueKey)) {
                error(
                                response,
                                ESTATISTICAS,
                                excelRow,
                                "team_side",
                                "Já existe uma linha de estatísticas para este fixture_ref e lado."
                );
            }
            statistics.add(item);
        }
    }

    private void validateStatistics(StatisticsRow item, SpreadsheetImportResponse response) {
        required(item.fixtureRef, ESTATISTICAS, item.rowNumber, "fixture_ref", response);
        required(item.teamSide, ESTATISTICAS, item.rowNumber, "team_side", response);
        if (!Set.of("HOME", "AWAY").contains(item.teamSide)) {
            error(
                            response,
                            ESTATISTICAS,
                            item.rowNumber,
                            "team_side",
                            "Use HOME para mandante ou AWAY para visitante."
            );
        }

        validateNonNegative(item.shotsTotal, ESTATISTICAS, item.rowNumber, "shots_total", response);
        validateNonNegative(item.shotsOnGoal, ESTATISTICAS, item.rowNumber, "shots_on_goal", response);
        validateNonNegative(item.shotsOffGoal, ESTATISTICAS, item.rowNumber, "shots_off_goal", response);
        validateNonNegative(item.blockedShots, ESTATISTICAS, item.rowNumber, "blocked_shots", response);
        validateNonNegative(item.shotsInsideBox, ESTATISTICAS, item.rowNumber, "shots_inside_box", response);
        validateNonNegative(item.shotsOutsideBox, ESTATISTICAS, item.rowNumber, "shots_outside_box", response);
        validatePercentage(item.possession, ESTATISTICAS, item.rowNumber, "possession", response);
        validateNonNegative(item.corners, ESTATISTICAS, item.rowNumber, "corners", response);
        validateNonNegative(item.fouls, ESTATISTICAS, item.rowNumber, "fouls", response);
        validateNonNegative(item.yellowCards, ESTATISTICAS, item.rowNumber, "yellow_cards", response);
        validateNonNegative(item.redCards, ESTATISTICAS, item.rowNumber, "red_cards", response);
        validateNonNegative(item.saves, ESTATISTICAS, item.rowNumber, "saves", response);
        validateNonNegative(item.totalPasses, ESTATISTICAS, item.rowNumber, "total_passes", response);
        validateNonNegative(item.accuratePasses, ESTATISTICAS, item.rowNumber, "accurate_passes", response);
        validatePercentage(item.passAccuracy, ESTATISTICAS, item.rowNumber, "pass_accuracy", response);
        validateNonNegative(item.dangerousAttacks, ESTATISTICAS, item.rowNumber, "dangerous_attacks", response);
        validateNonNegative(item.assists, ESTATISTICAS, item.rowNumber, "assists", response);
        if (item.expectedGoals != null && item.expectedGoals < 0) {
            error(response, ESTATISTICAS, item.rowNumber, "expected_goals", "O valor não pode ser negativo.");
        }
        if (item.accuratePasses != null && item.totalPasses != null
                        && item.accuratePasses > item.totalPasses) {
            error(
                            response,
                            ESTATISTICAS,
                            item.rowNumber,
                            "accurate_passes",
                            "Passes certos não podem superar o total de passes."
            );
        }
    }

    private void parseSources(
                    Sheet sheet,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    List<SourceRow> sources,
                    SpreadsheetImportResponse response
    ) {
        Map<String, Integer> headers = readHeaders(
                        sheet,
                        FONTES_HEADERS,
                        formatter,
                        evaluator,
                        response
        );
        if (headers.isEmpty()) {
            return;
        }

        validateRowLimit(sheet, FONTES, response);
        Set<String> uniqueRows = new HashSet<>();
        int upperBound = Math.min(sheet.getLastRowNum(), MAX_DATA_ROWS);

        for (int rowIndex = 1; rowIndex <= upperBound; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlank(row, formatter, evaluator)) {
                continue;
            }

            int excelRow = rowIndex + 1;
            SourceRow item = new SourceRow();
            item.rowNumber = excelRow;
            item.fixtureRef = text(row, headers, "fixture_ref", formatter, evaluator);
            item.sourceType = upper(text(row, headers, "source_type", formatter, evaluator));
            item.sourceUrl = text(row, headers, "source_url", formatter, evaluator);
            item.collectedAt = dateTime(row, headers, "collected_at", formatter, evaluator, response, FONTES, excelRow);
            item.confidence = decimal(row, headers, "confidence", formatter, evaluator, response, FONTES, excelRow);
            item.notes = text(row, headers, "notes", formatter, evaluator);

            validateSource(item, response);
            String uniqueKey = normalizeKey(item.fixtureRef) + "|" + normalizeKey(item.sourceUrl);
            if (!uniqueRows.add(uniqueKey)) {
                warning(
                                response,
                                FONTES,
                                excelRow,
                                "source_url",
                                "Fonte repetida para a mesma partida; apenas uma será registrada."
                );
            }
            sources.add(item);
        }
    }

    private void validateSource(SourceRow item, SpreadsheetImportResponse response) {
        required(item.fixtureRef, FONTES, item.rowNumber, "fixture_ref", response);
        required(item.sourceType, FONTES, item.rowNumber, "source_type", response);
        required(item.sourceUrl, FONTES, item.rowNumber, "source_url", response);
        if (item.collectedAt == null) {
            error(response, FONTES, item.rowNumber, "collected_at", "Informe data/hora ISO 8601 com fuso.");
        }
        if (item.confidence == null || item.confidence < 0 || item.confidence > 1) {
            error(response, FONTES, item.rowNumber, "confidence", "Use confiança entre 0 e 1.");
        }
        if (notBlank(item.sourceUrl)) {
            try {
                URI uri = URI.create(item.sourceUrl);
                if (!Set.of("http", "https").contains(lower(uri.getScheme()))) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                error(response, FONTES, item.rowNumber, "source_url", "URL inválida. Use http:// ou https://.");
            }
        }
    }

    private void validateRelationships(
                    Map<String, FixtureRow> fixtures,
                    List<StatisticsRow> statistics,
                    List<SourceRow> sources,
                    SpreadsheetImportResponse response
    ) {
        if (fixtures.isEmpty()) {
            error(
                            response,
                            PARTIDAS,
                            null,
                            null,
                            "Preencha pelo menos uma partida antes de importar."
            );
            return;
        }

        Map<String, Integer> sourceCounts = new HashMap<>();
        Set<String> statisticsSides = new HashSet<>();

        for (StatisticsRow item : statistics) {
            String fixtureKey = normalizeKey(item.fixtureRef);
            FixtureRow fixture = fixtures.get(fixtureKey);
            if (fixture == null) {
                error(
                                response,
                                ESTATISTICAS,
                                item.rowNumber,
                                "fixture_ref",
                                "fixture_ref não encontrado na aba PARTIDAS."
                );
                continue;
            }
            statisticsSides.add(fixtureKey + "|" + item.teamSide);
            if (!FINAL_STATUSES.contains(fixture.status)) {
                error(
                                response,
                                ESTATISTICAS,
                                item.rowNumber,
                                "fixture_ref",
                                "Estatísticas da partida só podem ser importadas com status FT, AET ou PEN."
                );
            }
        }

        for (SourceRow item : sources) {
            String fixtureKey = normalizeKey(item.fixtureRef);
            if (!fixtures.containsKey(fixtureKey)) {
                error(
                                response,
                                FONTES,
                                item.rowNumber,
                                "fixture_ref",
                                "fixture_ref não encontrado na aba PARTIDAS."
                );
                continue;
            }
            sourceCounts.merge(fixtureKey, 1, Integer::sum);
        }

        for (Map.Entry<String, FixtureRow> entry : fixtures.entrySet()) {
            FixtureRow item = entry.getValue();
            item.homeStatistics = statisticsSides.contains(entry.getKey() + "|HOME");
            item.awayStatistics = statisticsSides.contains(entry.getKey() + "|AWAY");
            item.sourceCount = sourceCounts.getOrDefault(entry.getKey(), 0);
            if (item.sourceCount == 0) {
                error(
                                response,
                                PARTIDAS,
                                item.rowNumber,
                                "fixture_ref",
                                "Cada partida precisa de pelo menos uma URL na aba FONTES."
                );
            }
            if (FINAL_STATUSES.contains(item.status)
                            && item.homeStatistics != item.awayStatistics) {
                warning(
                                response,
                                ESTATISTICAS,
                                null,
                                "team_side",
                                "A partida " + item.fixtureRef + " possui estatística de apenas um dos times."
                );
            }
        }
    }

    private void validateExistingIdentities(
                    Collection<FixtureRow> fixtures,
                    SpreadsheetImportResponse response
    ) {
        Set<String> announcedNewTeams = new HashSet<>();
        for (FixtureRow row : fixtures) {
            if (row.leagueApiId != null) {
                leagueRepository.findByApiId(row.leagueApiId).ifPresent(existing -> {
                    validateExistingIdentity(
                                    existing.getName(),
                                    row.leagueName,
                                    PARTIDAS,
                                    row.rowNumber,
                                    "league_api_id",
                                    response
                    );
                    validateExistingIdentity(
                                    existing.getCountry(),
                                    row.leagueCountry,
                                    PARTIDAS,
                                    row.rowNumber,
                                    "league_country",
                                    response
                    );
                });
            }

            validateExistingTeamIdentity(
                            row.homeTeamApiId,
                            row.homeTeamName,
                            row.homeTeamCountry,
                            row.rowNumber,
                            "home_team_api_id",
                            response
            );
            validateTeamResolution(
                            row.homeTeamApiId,
                            row.homeTeamName,
                            row.homeTeamCountry,
                            row.rowNumber,
                            "home_team_name",
                            announcedNewTeams,
                            response
            );
            validateExistingTeamIdentity(
                            row.awayTeamApiId,
                            row.awayTeamName,
                            row.awayTeamCountry,
                            row.rowNumber,
                            "away_team_api_id",
                            response
            );
            validateTeamResolution(
                            row.awayTeamApiId,
                            row.awayTeamName,
                            row.awayTeamCountry,
                            row.rowNumber,
                            "away_team_name",
                            announcedNewTeams,
                            response
            );

            if (row.fixtureApiId == null) {
                continue;
            }
            Optional<Fixture> existing = fixtureRepository.findByApiId(row.fixtureApiId);
            if (existing.isEmpty()) {
                continue;
            }

            Optional<League> league =
                            resolveLeague(row.leagueApiId, row.leagueName, row.leagueCountry);
            Optional<Team> home =
                            resolveTeam(row.homeTeamApiId, row.homeTeamName, row.homeTeamCountry);
            Optional<Team> away =
                            resolveTeam(row.awayTeamApiId, row.awayTeamName, row.awayTeamCountry);

            boolean sameFixture = league.isPresent()
                            && home.isPresent()
                            && away.isPresent()
                            && row.kickoffAt != null
                            && Objects.equals(existing.get().getLeagueId(), league.get().getId())
                            && Objects.equals(existing.get().getHomeTeamId(), home.get().getId())
                            && Objects.equals(existing.get().getAwayTeamId(), away.get().getId())
                            && existing.get().getDate() != null
                            && existing.get().getDate().toInstant().equals(row.kickoffAt.toInstant());
            if (!sameFixture) {
                error(
                                response,
                                PARTIDAS,
                                row.rowNumber,
                                "fixture_api_id",
                                "O fixture_api_id informado já pertence a outra partida."
                );
            }
        }
    }

    private void validateTeamResolution(
                    Integer apiId,
                    String name,
                    String country,
                    int rowNumber,
                    String field,
                    Set<String> announcedNewTeams,
                    SpreadsheetImportResponse response
    ) {
        if (!notBlank(name) || !notBlank(country)) {
            return;
        }

        TeamLookup lookup = lookupTeam(apiId, name, country);
        if (lookup.ambiguous()) {
            error(
                            response,
                            PARTIDAS,
                            rowNumber,
                            field,
                            "Há mais de um time compatível com '" + name
                                            + "'. Informe o api_id exibido na aba "
                                            + TIMES_CADASTRADOS + "."
            );
            return;
        }
        if (lookup.team().isPresent()) {
            return;
        }

        String warningKey = normalizeTeamName(name) + "|" + normalizeKey(country);
        if (announcedNewTeams.add(warningKey)) {
            warning(
                            response,
                            PARTIDAS,
                            rowNumber,
                            field,
                            "Time novo: '" + name + "' (" + country
                                            + ") será cadastrado automaticamente no fim da tabela teams."
            );
        }
    }

    private void validateExistingTeamIdentity(
                    Integer apiId,
                    String name,
                    String country,
                    int rowNumber,
                    String field,
                    SpreadsheetImportResponse response
    ) {
        if (apiId == null) {
            return;
        }
        teamRepository.findByApiId(apiId).ifPresent(existing -> {
            validateExistingIdentity(
                            existing.getName(),
                            name,
                            PARTIDAS,
                            rowNumber,
                            field,
                            response
            );
            validateExistingIdentity(
                            existing.getCountry(),
                            country,
                            PARTIDAS,
                            rowNumber,
                            field,
                            response
            );
        });
    }

    private void validateExistingIdentity(
                    String existing,
                    String incoming,
                    String sheet,
                    Integer row,
                    String field,
                    SpreadsheetImportResponse response
    ) {
        if (notBlank(existing) && notBlank(incoming) && !sameNormalized(existing, incoming)) {
            error(
                            response,
                            sheet,
                            row,
                            field,
                            "O identificador informado já pertence a '" + existing
                                            + "', não a '" + incoming + "'."
            );
        }
    }

    private void populatePreview(ParsedWorkbook parsed) {
        if (!parsed.response().getFixtures().isEmpty()) {
            return;
        }
        for (FixtureRow item : parsed.fixtures().values()) {
            parsed.response().getFixtures().add(
                            FixturePreview.builder()
                                            .fixtureRef(item.fixtureRef)
                                            .action(resolveAction(item))
                                            .leagueName(item.leagueName)
                                            .leagueCountry(item.leagueCountry)
                                            .kickoffAt(item.kickoffAt)
                                            .status(item.status)
                                            .homeTeamName(item.homeTeamName)
                                            .awayTeamName(item.awayTeamName)
                                            .homeGoals(item.homeGoals)
                                            .awayGoals(item.awayGoals)
                                            .homeStatistics(item.homeStatistics)
                                            .awayStatistics(item.awayStatistics)
                                            .sourceCount(item.sourceCount)
                                            .build()
            );
        }
    }

    private String resolveAction(FixtureRow item) {
        if (item.fixtureApiId != null && fixtureRepository.findByApiId(item.fixtureApiId).isPresent()) {
            return "ATUALIZAR";
        }

        Optional<League> league = resolveLeague(item.leagueApiId, item.leagueName, item.leagueCountry);
        Optional<Team> home = resolveTeam(item.homeTeamApiId, item.homeTeamName, item.homeTeamCountry);
        Optional<Team> away = resolveTeam(item.awayTeamApiId, item.awayTeamName, item.awayTeamCountry);

        if (league.isEmpty() || home.isEmpty() || away.isEmpty() || item.kickoffAt == null) {
            return "INSERIR";
        }

        return fixtureRepository.findFirstByLeagueIdAndHomeTeamIdAndAwayTeamIdAndDate(
                        league.get().getId(),
                        home.get().getId(),
                        away.get().getId(),
                        item.kickoffAt
        ).isPresent() ? "ATUALIZAR" : "INSERIR";
    }

    private void persist(ParsedWorkbook parsed) {
        SpreadsheetImportResponse response = parsed.response();
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, League> leagueCache = new HashMap<>();
        Map<String, Team> teamCache = new HashMap<>();
        Map<String, Fixture> fixtureCache = new HashMap<>();
        Set<Long> affectedTeams = new HashSet<>();

        for (Map.Entry<String, FixtureRow> entry : parsed.fixtures().entrySet()) {
            FixtureRow row = entry.getValue();
            League league = upsertLeague(row, leagueCache, response, now);
            Team home = upsertTeam(
                            row.homeTeamApiId,
                            row.homeTeamName,
                            row.homeTeamCountry,
                            row.rowNumber,
                            teamCache,
                            response,
                            now
            );
            Team away = upsertTeam(
                            row.awayTeamApiId,
                            row.awayTeamName,
                            row.awayTeamCountry,
                            row.rowNumber,
                            teamCache,
                            response,
                            now
            );

            Fixture fixture = findFixture(row, league, home, away, response).orElse(null);
            boolean inserted = fixture == null;
            if (inserted) {
                fixture = new Fixture();
                fixture.setApiId(
                                row.fixtureApiId != null
                                                ? row.fixtureApiId
                                                : availableNegativeId(
                                                                "fixture:" + normalizeKey(row.fixtureRef),
                                                                fixtureRepository::findByApiId
                                                )
                );
                fixture.setCreatedAt(now);
                response.setFixturesInserted(response.getFixturesInserted() + 1);
            } else {
                response.setFixturesUpdated(response.getFixturesUpdated() + 1);
            }

            fixture.setLeagueId(league.getId());
            fixture.setHomeTeamId(home.getId());
            fixture.setAwayTeamId(away.getId());
            fixture.setDate(row.kickoffAt);
            applyFixtureStatusAndScore(fixture, row, response);
            fixture.setUpdatedAt(now);
            fixture = fixtureRepository.save(fixture);

            fixtureCache.put(entry.getKey(), fixture);
            affectedTeams.add(home.getId());
            affectedTeams.add(away.getId());
        }

        for (StatisticsRow row : parsed.statistics()) {
            Fixture fixture = fixtureCache.get(normalizeKey(row.fixtureRef));
            if (fixture == null) {
                continue;
            }
            Long teamId = "HOME".equals(row.teamSide)
                            ? fixture.getHomeTeamId()
                            : fixture.getAwayTeamId();

            MatchStatistics stats = statisticsRepository.findFirstByFixtureIdAndTeamId(
                            fixture.getId(),
                            teamId
            );
            if (stats == null) {
                stats = new MatchStatistics();
                stats.setFixtureId(fixture.getId());
                stats.setTeamId(teamId);
                stats.setCreatedAt(now);
            }

            applyStatistics(stats, row);
            statisticsRepository.save(stats);
            applyLegacyFixtureStatistics(fixture, row);
            fixture.setUpdatedAt(now);
            fixtureRepository.save(fixture);
            response.setStatisticsUpserted(response.getStatisticsUpserted() + 1);
        }

        Set<String> sourceKeys = new HashSet<>();
        for (SourceRow row : parsed.sources()) {
            Fixture fixture = fixtureCache.get(normalizeKey(row.fixtureRef));
            if (fixture == null) {
                continue;
            }
            String endpoint = "XLSX_IMPORT:" + row.sourceType;
            String message = sourceMessage(row);
            String sourceKey = fixture.getId() + "|" + endpoint + "|" + message;
            if (!sourceKeys.add(sourceKey)) {
                continue;
            }
            if (!statsLogRepository.existsByFixtureIdAndEndpointAndMessage(
                            fixture.getId(),
                            endpoint,
                            message
            )) {
                StatsLog log = new StatsLog();
                log.setFixtureId(fixture.getId());
                log.setEndpoint(endpoint);
                log.setStatus("SUCCESS");
                log.setMessage(message);
                log.setCreatedAt(row.collectedAt != null ? row.collectedAt : now);
                statsLogRepository.save(log);
                response.setSourcesInserted(response.getSourcesInserted() + 1);
            }
        }

        recomputeRecentTeamStatistics(affectedTeams, now);
    }

    private League upsertLeague(
                    FixtureRow row,
                    Map<String, League> cache,
                    SpreadsheetImportResponse response,
                    OffsetDateTime now
    ) {
        String cacheKey = row.leagueApiId != null
                        ? "id:" + row.leagueApiId
                        : "name:" + normalizeKey(row.leagueName) + "|" + normalizeKey(row.leagueCountry);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        League league = resolveLeague(row.leagueApiId, row.leagueName, row.leagueCountry)
                        .orElse(null);
        if (league == null) {
            league = new League();
            league.setApiId(
                            row.leagueApiId != null
                                            ? row.leagueApiId
                                            : availableNegativeId(
                                                            "league:" + cacheKey,
                                                            leagueRepository::findByApiId
                                            )
            );
            response.setLeaguesInserted(response.getLeaguesInserted() + 1);
        } else {
            assertSameIdentity(
                            league.getName(),
                            row.leagueName,
                            PARTIDAS,
                            row.rowNumber,
                            "league_api_id",
                            response
            );
            assertSameIdentity(
                            league.getCountry(),
                            row.leagueCountry,
                            PARTIDAS,
                            row.rowNumber,
                            "league_country",
                            response
            );
        }
        league.setName(row.leagueName.trim());
        league.setCountry(row.leagueCountry.trim());
        league.setUpdatedAt(now);
        league = leagueRepository.save(league);
        cache.put(cacheKey, league);
        return league;
    }

    private Team upsertTeam(
                    Integer apiId,
                    String name,
                    String country,
                    int rowNumber,
                    Map<String, Team> cache,
                    SpreadsheetImportResponse response,
                    OffsetDateTime now
    ) {
        String cacheKey = apiId != null
                        ? "id:" + apiId
                        : "name:" + normalizeKey(name) + "|" + normalizeKey(country);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        TeamLookup lookup = lookupTeam(apiId, name, country);
        if (lookup.ambiguous()) {
            error(
                            response,
                            PARTIDAS,
                            rowNumber,
                            "team_api_id",
                            "Há mais de um time compatível com '" + name
                                            + "'. Informe o api_id exibido na aba "
                                            + TIMES_CADASTRADOS + "."
            );
            throw new SpreadsheetImportException(
                            "Não foi possível identificar o time sem ambiguidade.",
                            response
            );
        }

        Team team = lookup.team().orElse(null);
        if (team == null) {
            team = new Team();
            team.setApiId(
                            apiId != null
                                            ? apiId
                                            : availableNegativeId(
                                                            "team:" + cacheKey,
                                                            teamRepository::findByApiId
                                            )
            );
            response.setTeamsInserted(response.getTeamsInserted() + 1);
        } else {
            assertSameIdentity(
                            team.getName(),
                            name,
                            PARTIDAS,
                            rowNumber,
                            "team_api_id",
                            response
            );
            assertSameIdentity(
                            team.getCountry(),
                            country,
                            PARTIDAS,
                            rowNumber,
                            "team_country",
                            response
            );
        }
        team.setName(name.trim());
        team.setCountry(country.trim());
        team.setUpdatedAt(now);
        team = teamRepository.save(team);
        cache.put(cacheKey, team);
        return team;
    }

    private Optional<League> resolveLeague(Integer apiId, String name, String country) {
        if (apiId != null) {
            Optional<League> byId = leagueRepository.findByApiId(apiId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (notBlank(name) && notBlank(country)) {
            return leagueRepository.findFirstByNameIgnoreCaseAndCountryIgnoreCase(
                            name.trim(),
                            country.trim()
            );
        }
        return Optional.empty();
    }

    private Optional<Team> resolveTeam(Integer apiId, String name, String country) {
        return lookupTeam(apiId, name, country).team();
    }

    private TeamLookup lookupTeam(Integer apiId, String name, String country) {
        if (apiId != null) {
            Optional<Team> byId = teamRepository.findByApiId(apiId);
            if (byId.isPresent()) {
                return TeamLookup.found(byId.get());
            }
        }

        if (!notBlank(name)) {
            return TeamLookup.notFound();
        }

        List<Team> candidates = new ArrayList<>(
                        teamRepository.findAllByNameIgnoreCase(name.trim())
        );
        if (candidates.isEmpty()) {
            String normalizedName = normalizeTeamName(name);
            candidates = teamRepository.findAll().stream()
                            .filter(team -> normalizedName.equals(
                                            normalizeTeamName(team.getName())
                            ))
                            .toList();
        }
        if (candidates.isEmpty()) {
            return TeamLookup.notFound();
        }

        if (notBlank(country)) {
            List<Team> countryMatches = candidates.stream()
                            .filter(team -> sameNormalized(team.getCountry(), country))
                            .toList();
            if (countryMatches.size() == 1) {
                return TeamLookup.found(countryMatches.get(0));
            }
            if (countryMatches.size() > 1) {
                return TeamLookup.ambiguousResult();
            }

            List<Team> withoutCountry = candidates.stream()
                            .filter(team -> !notBlank(team.getCountry()))
                            .toList();
            if (withoutCountry.size() == 1) {
                return TeamLookup.found(withoutCountry.get(0));
            }
            if (withoutCountry.size() > 1) {
                return TeamLookup.ambiguousResult();
            }
        }

        if (candidates.size() == 1 && !notBlank(country)) {
            return TeamLookup.found(candidates.get(0));
        }
        return candidates.size() > 1
                        ? TeamLookup.ambiguousResult()
                        : TeamLookup.notFound();
    }

    private Optional<Fixture> findFixture(
                    FixtureRow row,
                    League league,
                    Team home,
                    Team away,
                    SpreadsheetImportResponse response
    ) {
        if (row.fixtureApiId != null) {
            Optional<Fixture> byId = fixtureRepository.findByApiId(row.fixtureApiId);
            if (byId.isPresent()) {
                assertSameFixture(byId.get(), row, league, home, away, response);
                return byId;
            }
        } else {
            int generatedId = negativeId("fixture:" + normalizeKey(row.fixtureRef));
            Optional<Fixture> byGeneratedId = fixtureRepository.findByApiId(generatedId);
            if (byGeneratedId.isPresent()) {
                assertSameFixture(byGeneratedId.get(), row, league, home, away, response);
                return byGeneratedId;
            }
        }
        return fixtureRepository.findFirstByLeagueIdAndHomeTeamIdAndAwayTeamIdAndDate(
                        league.getId(),
                        home.getId(),
                        away.getId(),
                        row.kickoffAt
        );
    }

    private void assertSameFixture(
                    Fixture existing,
                    FixtureRow incoming,
                    League league,
                    Team home,
                    Team away,
                    SpreadsheetImportResponse response
    ) {
        boolean sameIdentity = Objects.equals(existing.getLeagueId(), league.getId())
                        && Objects.equals(existing.getHomeTeamId(), home.getId())
                        && Objects.equals(existing.getAwayTeamId(), away.getId())
                        && existing.getDate() != null
                        && incoming.kickoffAt != null
                        && existing.getDate().toInstant().equals(incoming.kickoffAt.toInstant());
        if (sameIdentity) {
            return;
        }

        error(
                        response,
                        PARTIDAS,
                        incoming.rowNumber,
                        "fixture_api_id",
                        "O fixture_api_id informado já pertence a outra partida."
        );
        throw new SpreadsheetImportException(
                        "Conflito de identificador da partida.",
                        response
        );
    }

    private void applyFixtureStatusAndScore(
                    Fixture fixture,
                    FixtureRow row,
                    SpreadsheetImportResponse response
    ) {
        if (fixture.getStatus() != null
                && FINAL_STATUSES.contains(fixture.getStatus())
                && "NS".equals(row.status)) {
            warning(
                            response,
                            PARTIDAS,
                            row.rowNumber,
                            "status",
                            "O banco já possui resultado encerrado; o status NS não substituiu o FT existente."
            );
            return;
        }
        fixture.setStatus(row.status);
        if (row.homeGoals != null) {
            fixture.setHomeGoals(row.homeGoals);
        }
        if (row.awayGoals != null) {
            fixture.setAwayGoals(row.awayGoals);
        }
    }

    private void applyStatistics(MatchStatistics target, StatisticsRow source) {
        setIfPresent(source.shotsTotal, target::setShotsTotal);
        setIfPresent(source.shotsOnGoal, target::setShotsOnGoal);
        setIfPresent(source.shotsOffGoal, target::setShotsOffGoal);
        setIfPresent(source.blockedShots, target::setBlockedShots);
        setIfPresent(source.shotsInsideBox, target::setShotsInsideBox);
        setIfPresent(source.shotsOutsideBox, target::setShotsOutsideBox);
        setIfPresentFloat(source.possession, target::setPossession);
        setIfPresent(source.corners, target::setCorners);
        setIfPresent(source.fouls, target::setFouls);
        setIfPresent(source.yellowCards, target::setYellowCards);
        setIfPresent(source.redCards, target::setRedCards);
        setIfPresent(source.saves, target::setSaves);
        setIfPresent(source.totalPasses, target::setTotalPasses);
        setIfPresent(source.accuratePasses, target::setAccuratePasses);
        setIfPresentFloat(source.passAccuracy, target::setPassAccuracy);
        setIfPresentFloat(source.expectedGoals, target::setExpectedGoals);
        setIfPresent(source.dangerousAttacks, target::setDangerousAttacks);
    }

    private void applyLegacyFixtureStatistics(Fixture fixture, StatisticsRow source) {
        boolean home = "HOME".equals(source.teamSide);
        if (home) {
            if (source.shotsTotal != null) fixture.setHomeShotsTotal(source.shotsTotal);
            if (source.shotsOnGoal != null) fixture.setHomeShotsOnTarget(source.shotsOnGoal);
            if (source.corners != null) fixture.setHomeCorners(source.corners);
            if (source.yellowCards != null) fixture.setHomeYellow(source.yellowCards);
            if (source.redCards != null) fixture.setHomeRed(source.redCards);
            if (source.possession != null) fixture.setHomePossession(source.possession);
            if (source.passAccuracy != null) fixture.setHomePassAccuracy(source.passAccuracy);
            if (source.assists != null) fixture.setHomeAssists(source.assists);
        } else {
            if (source.shotsTotal != null) fixture.setAwayShotsTotal(source.shotsTotal);
            if (source.shotsOnGoal != null) fixture.setAwayShotsOnTarget(source.shotsOnGoal);
            if (source.corners != null) fixture.setAwayCorners(source.corners);
            if (source.yellowCards != null) fixture.setAwayYellow(source.yellowCards);
            if (source.redCards != null) fixture.setAwayRed(source.redCards);
            if (source.possession != null) fixture.setAwayPossession(source.possession);
            if (source.passAccuracy != null) fixture.setAwayPassAccuracy(source.passAccuracy);
            if (source.assists != null) fixture.setAwayAssists(source.assists);
        }
    }

    private void recomputeRecentTeamStatistics(Set<Long> teamIds, OffsetDateTime now) {
        for (Long teamId : teamIds) {
            List<Fixture> completed = fixtureRepository.findCompletedByTeamId(teamId);
            if (completed.size() > 10) {
                completed = completed.subList(0, 10);
            }
            if (completed.isEmpty()) {
                continue;
            }

            List<Integer> goalsFor = new ArrayList<>();
            List<Integer> goalsAgainst = new ArrayList<>();
            for (Fixture fixture : completed) {
                boolean home = Objects.equals(fixture.getHomeTeamId(), teamId);
                goalsFor.add(home ? fixture.getHomeGoals() : fixture.getAwayGoals());
                goalsAgainst.add(home ? fixture.getAwayGoals() : fixture.getHomeGoals());
            }

            double avgFor = goalsFor.stream().mapToInt(Integer::intValue).average().orElse(0);
            double avgAgainst = goalsAgainst.stream().mapToInt(Integer::intValue).average().orElse(0);
            List<Fixture> teamFixtures = fixtureRepository.findAllByTeamId(teamId);
            for (Fixture fixture : teamFixtures) {
                if (Objects.equals(fixture.getHomeTeamId(), teamId)) {
                    fixture.setHomeAvgScored(avgFor);
                    fixture.setHomeAvgConceded(avgAgainst);
                    fixture.setHomeRecentFor(new ArrayList<>(goalsFor));
                    fixture.setHomeRecentAgainst(new ArrayList<>(goalsAgainst));
                }
                if (Objects.equals(fixture.getAwayTeamId(), teamId)) {
                    fixture.setAwayAvgScored(avgFor);
                    fixture.setAwayAvgConceded(avgAgainst);
                    fixture.setAwayRecentFor(new ArrayList<>(goalsFor));
                    fixture.setAwayRecentAgainst(new ArrayList<>(goalsAgainst));
                }
                fixture.setUpdatedAt(now);
            }
            fixtureRepository.saveAll(teamFixtures);
        }
    }

    private String sourceMessage(SourceRow row) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("url", row.sourceUrl);
        message.put("confidence", row.confidence);
        message.put("collected_at", row.collectedAt != null ? row.collectedAt.toString() : null);
        message.put("notes", row.notes);
        return json(message);
    }

    private Map<String, Integer> readHeaders(
                    Sheet sheet,
                    List<String> expected,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    SpreadsheetImportResponse response
    ) {
        if (sheet == null) {
            return Map.of();
        }
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            error(response, sheet.getSheetName(), 1, null, "Cabeçalho ausente.");
            return Map.of();
        }

        Map<String, Integer> headers = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = normalizeHeader(formatter.formatCellValue(cell, evaluator));
            if (!value.isBlank()) {
                headers.put(value, cell.getColumnIndex());
            }
        }

        for (String header : expected) {
            if (!headers.containsKey(header)) {
                error(
                                response,
                                sheet.getSheetName(),
                                1,
                                header,
                                "Coluna obrigatória ausente: " + header + "."
                );
            }
        }
        return hasSheetHeaderErrors(response, sheet.getSheetName()) ? Map.of() : headers;
    }

    private void validateRowLimit(
                    Sheet sheet,
                    String sheetName,
                    SpreadsheetImportResponse response
    ) {
        if (sheet.getLastRowNum() > MAX_DATA_ROWS) {
            error(
                            response,
                            sheetName,
                            null,
                            null,
                            "A aba excede o limite de " + MAX_DATA_ROWS + " linhas."
            );
        }
    }

    private String text(
                    Row row,
                    Map<String, Integer> headers,
                    String header,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator
    ) {
        Integer column = headers.get(header);
        if (column == null) {
            return null;
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell, evaluator);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Integer integer(
                    Row row,
                    Map<String, Integer> headers,
                    String header,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    SpreadsheetImportResponse response,
                    String sheet,
                    int excelRow
    ) {
        Integer column = headers.get(header);
        if (column == null) {
            return null;
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                double raw = cell.getNumericCellValue();
                if (raw != Math.rint(raw)) {
                    throw new NumberFormatException();
                }
                return Math.toIntExact((long) raw);
            }
            String value = formatter.formatCellValue(cell, evaluator).trim();
            if (value.isEmpty()) {
                return null;
            }
            return Integer.valueOf(value.replaceFirst("[.,]0+$", ""));
        } catch (ArithmeticException | NumberFormatException ex) {
            error(response, sheet, excelRow, header, "Informe um número inteiro válido.");
            return null;
        }
    }

    private Double decimal(
                    Row row,
                    Map<String, Integer> headers,
                    String header,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    SpreadsheetImportResponse response,
                    String sheet,
                    int excelRow
    ) {
        Integer column = headers.get(header);
        if (column == null) {
            return null;
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            }
            String value = formatter.formatCellValue(cell, evaluator).trim();
            if (value.isEmpty()) {
                return null;
            }
            return Double.valueOf(value.replace("%", "").replace(",", "."));
        } catch (NumberFormatException ex) {
            error(response, sheet, excelRow, header, "Informe um número decimal válido.");
            return null;
        }
    }

    private OffsetDateTime dateTime(
                    Row row,
                    Map<String, Integer> headers,
                    String header,
                    DataFormatter formatter,
                    FormulaEvaluator evaluator,
                    SpreadsheetImportResponse response,
                    String sheet,
                    int excelRow
    ) {
        String value = text(row, headers, header, formatter, evaluator);
        if (!notBlank(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (RuntimeException ex) {
            error(
                            response,
                            sheet,
                            excelRow,
                            header,
                            "Use ISO 8601 com fuso, por exemplo 2026-07-29T21:30:00-03:00."
            );
            return null;
        }
    }

    private boolean isBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell, evaluator).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void required(
                    String value,
                    String sheet,
                    Integer row,
                    String field,
                    SpreadsheetImportResponse response
    ) {
        if (!notBlank(value)) {
            error(response, sheet, row, field, "Campo obrigatório.");
        }
    }

    private void validateNonNegative(
                    Integer value,
                    String sheet,
                    Integer row,
                    String field,
                    SpreadsheetImportResponse response
    ) {
        if (value != null && value < 0) {
            error(response, sheet, row, field, "O valor não pode ser negativo.");
        }
    }

    private void validatePercentage(
                    Double value,
                    String sheet,
                    Integer row,
                    String field,
                    SpreadsheetImportResponse response
    ) {
        if (value != null && (value < 0 || value > 100)) {
            error(response, sheet, row, field, "Use um valor entre 0 e 100.");
        }
    }

    private void assertSameIdentity(
                    String existing,
                    String incoming,
                    String sheet,
                    Integer row,
                    String field,
                    SpreadsheetImportResponse response
    ) {
        if (notBlank(existing) && notBlank(incoming) && !sameNormalized(existing, incoming)) {
            error(
                            response,
                            sheet,
                            row,
                            field,
                            "O API ID informado já pertence a '" + existing + "', não a '" + incoming + "'."
            );
            throw new SpreadsheetImportException(
                            "Conflito de identificador na planilha.",
                            response
            );
        }
    }

    private int availableNegativeId(
                    String seed,
                    IntFunction<? extends Optional<?>> finder
    ) {
        int candidate = negativeId(seed);
        while (finder.apply(candidate).isPresent()) {
            candidate--;
            if (candidate == Integer.MIN_VALUE) {
                candidate = -1;
            }
        }
        return candidate;
    }

    private int negativeId(String seed) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                            .digest(seed.getBytes(StandardCharsets.UTF_8));
            int raw = ByteBuffer.wrap(digest).getInt();
            return -1 - Math.floorMod(raw, 2_000_000_000);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar os dados importados.", ex);
        }
    }

    private void setIfPresent(Integer value, java.util.function.Consumer<Integer> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void setIfPresentFloat(Double value, java.util.function.Consumer<Float> setter) {
        if (value != null) {
            setter.accept(value.floatValue());
        }
    }

    private boolean hasErrors(SpreadsheetImportResponse response) {
        return response.getIssues().stream().anyMatch(issue -> "ERROR".equals(issue.getSeverity()));
    }

    private boolean hasSheetHeaderErrors(SpreadsheetImportResponse response, String sheet) {
        return response.getIssues().stream().anyMatch(
                        issue -> "ERROR".equals(issue.getSeverity())
                                        && Objects.equals(sheet, issue.getSheet())
                                        && Objects.equals(1, issue.getRow())
        );
    }

    private void error(
                    SpreadsheetImportResponse response,
                    String sheet,
                    Integer row,
                    String field,
                    String message
    ) {
        response.getIssues().add(
                        Issue.builder()
                                        .severity("ERROR")
                                        .sheet(sheet)
                                        .row(row)
                                        .field(field)
                                        .message(message)
                                        .build()
        );
        response.setValid(false);
    }

    private void warning(
                    SpreadsheetImportResponse response,
                    String sheet,
                    Integer row,
                    String field,
                    String message
    ) {
        response.getIssues().add(
                        Issue.builder()
                                        .severity("WARNING")
                                        .sheet(sheet)
                                        .row(row)
                                        .field(field)
                                        .message(message)
                                        .build()
        );
    }

    private String normalizeHeader(String value) {
        return Optional.ofNullable(value)
                        .orElse("")
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        .replace(' ', '_');
    }

    private String normalizeKey(String value) {
        return Optional.ofNullable(value)
                        .orElse("")
                        .trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("\\s+", " ");
    }

    private String normalizeTeamName(String value) {
        String withoutAccents = Normalizer.normalize(
                        Optional.ofNullable(value).orElse(""),
                        Normalizer.Form.NFD
        ).replaceAll("\\p{M}+", "");
        return withoutAccents
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", " ")
                        .trim()
                        .replaceAll("\\s+", " ");
    }

    private boolean sameNormalized(String first, String second) {
        return notBlank(first) && notBlank(second)
                        && normalizeKey(first).equals(normalizeKey(second));
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String upper(String value) {
        return notBlank(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return notBlank(message) ? message : ex.getClass().getSimpleName();
    }

    private record ParsedWorkbook(
                    Map<String, FixtureRow> fixtures,
                    List<StatisticsRow> statistics,
                    List<SourceRow> sources,
                    SpreadsheetImportResponse response
    ) {
    }

    private record TeamLookup(Optional<Team> team, boolean ambiguous) {
        private static TeamLookup found(Team team) {
            return new TeamLookup(Optional.of(team), false);
        }

        private static TeamLookup notFound() {
            return new TeamLookup(Optional.empty(), false);
        }

        private static TeamLookup ambiguousResult() {
            return new TeamLookup(Optional.empty(), true);
        }
    }

    private static class FixtureRow {
        private int rowNumber;
        private String fixtureRef;
        private Integer fixtureApiId;
        private Integer leagueApiId;
        private String leagueName;
        private String leagueCountry;
        private OffsetDateTime kickoffAt;
        private String status;
        private Integer homeTeamApiId;
        private String homeTeamName;
        private String homeTeamCountry;
        private Integer awayTeamApiId;
        private String awayTeamName;
        private String awayTeamCountry;
        private Integer homeGoals;
        private Integer awayGoals;
        private boolean homeStatistics;
        private boolean awayStatistics;
        private int sourceCount;
    }

    private static class StatisticsRow {
        private int rowNumber;
        private String fixtureRef;
        private String teamSide;
        private Integer shotsTotal;
        private Integer shotsOnGoal;
        private Integer shotsOffGoal;
        private Integer blockedShots;
        private Integer shotsInsideBox;
        private Integer shotsOutsideBox;
        private Double possession;
        private Integer corners;
        private Integer fouls;
        private Integer yellowCards;
        private Integer redCards;
        private Integer saves;
        private Integer totalPasses;
        private Integer accuratePasses;
        private Double passAccuracy;
        private Double expectedGoals;
        private Integer dangerousAttacks;
        private Integer assists;
    }

    private static class SourceRow {
        private int rowNumber;
        private String fixtureRef;
        private String sourceType;
        private String sourceUrl;
        private OffsetDateTime collectedAt;
        private Double confidence;
        private String notes;
    }
}
