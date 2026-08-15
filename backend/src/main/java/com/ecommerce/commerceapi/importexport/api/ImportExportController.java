package com.ecommerce.commerceapi.importexport.api;

import com.ecommerce.commerceapi.importexport.service.ImportExportService;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/import-export")
public class ImportExportController {
    private final ImportExportService service;

    public ImportExportController(ImportExportService service) {
        this.service = service;
    }

    @GetMapping("/{module}/template")
    public ResponseEntity<byte[]> template(@PathVariable String module, @RequestParam(defaultValue = "xlsx") String format) {
        ImportExportModule selectedModule = ImportExportModule.from(module);
        ImportExportFormat selectedFormat = ImportExportFormat.from(format);
        byte[] payload = service.template(selectedModule, selectedFormat);
        return fileResponse(payload, selectedModule, "template", selectedFormat);
    }

    @GetMapping("/{module}/export")
    public ResponseEntity<byte[]> export(@PathVariable String module, @RequestParam(defaultValue = "xlsx") String format) {
        ImportExportModule selectedModule = ImportExportModule.from(module);
        ImportExportFormat selectedFormat = ImportExportFormat.from(format);
        byte[] payload = service.export(selectedModule, selectedFormat);
        return fileResponse(payload, selectedModule, "export", selectedFormat);
    }

    @PostMapping(value = "/{module}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportExportResult> importFile(@PathVariable String module, @RequestParam("file") MultipartFile file) {
        ImportExportModule selectedModule = ImportExportModule.from(module);
        return ResponseEntity.ok(service.importFile(selectedModule, file));
    }

    private ResponseEntity<byte[]> fileResponse(byte[] payload, ImportExportModule module, String action, ImportExportFormat format) {
        String filename = module.slug() + "-" + action + "-" + LocalDate.now() + "." + format.extension();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(format.contentType()))
                .body(payload);
    }
}
