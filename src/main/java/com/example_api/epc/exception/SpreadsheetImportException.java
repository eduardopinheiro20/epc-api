package com.example_api.epc.exception;

import com.example_api.epc.dto.SpreadsheetImportResponse;
import lombok.Getter;

@Getter
public class SpreadsheetImportException extends RuntimeException {

    private final SpreadsheetImportResponse response;

    public SpreadsheetImportException(String message, SpreadsheetImportResponse response) {
        super(message);
        this.response = response;
    }
}
