package com.example_api.epc.controller;

import com.example_api.epc.dto.SpreadsheetImportResponse;
import com.example_api.epc.service.SpreadsheetImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/importacoes/planilha")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SpreadsheetImportController {

    private static final String MODEL_FILENAME = "modelo_importacao_epc_ia.xlsx";

    private final SpreadsheetImportService importService;

    @PostMapping(
                    value = "/validar",
                    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<SpreadsheetImportResponse> validate(
                    @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importService.validate(file));
    }

    @PostMapping(
                    value = "/importar",
                    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<SpreadsheetImportResponse> importWorkbook(
                    @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importService.importWorkbook(file));
    }

    @GetMapping("/modelo")
    public ResponseEntity<byte[]> downloadModel() {
        byte[] model = importService.generateModel();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                        ContentDisposition.attachment().filename(MODEL_FILENAME).build()
        );
        return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.parseMediaType(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        ))
                        .contentLength(model.length)
                        .body(model);
    }
}
