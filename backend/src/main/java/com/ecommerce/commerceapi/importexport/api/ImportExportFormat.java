package com.ecommerce.commerceapi.importexport.api;

public enum ImportExportFormat {
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    CSV("text/csv", "csv");

    private final String contentType;
    private final String extension;

    ImportExportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static ImportExportFormat from(String value) {
        if (value == null || value.isBlank()) {
            return XLSX;
        }
        return ImportExportFormat.valueOf(value.trim().toUpperCase());
    }
}
