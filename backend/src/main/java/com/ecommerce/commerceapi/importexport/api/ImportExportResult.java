package com.ecommerce.commerceapi.importexport.api;

import java.util.List;

public record ImportExportResult(
        String module,
        int processed,
        int created,
        int updated,
        int failed,
        List<ImportExportError> errors) {
}
