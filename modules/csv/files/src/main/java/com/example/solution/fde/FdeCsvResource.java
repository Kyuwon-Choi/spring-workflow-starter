package com.example.solution.fde;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fde")
@Validated
@PreAuthorize("isAuthenticated()")
public class FdeCsvResource {
    public record Csv(@NotBlank @Size(max = 100000) String csv) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown envelope field"); }
    }
    public record Batch(@NotNull @Size(min = 1, max = 100) List<Map<String, Object>> rows) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown envelope field"); }
    }
    private final FdeCsvService service;
    public FdeCsvResource(FdeCsvService service) { this.service = service; }
    @PostMapping("/{domain}/{type}/import/preview")
    public FdeCsvService.Preview preview(@PathVariable String domain, @PathVariable String type, @Valid @RequestBody Csv input) {
        return service.preview(domain, type, input.csv());
    }
    @PostMapping("/{domain}/{type}/import") @ResponseStatus(HttpStatus.CREATED)
    public List<FdeService.View> importRows(@PathVariable String domain, @PathVariable String type, @Valid @RequestBody Batch input) {
        return service.importRows(domain, type, input.rows());
    }
    @GetMapping(value = "/{domain}/{type}/export", produces = "text/csv")
    public ResponseEntity<String> export(@PathVariable String domain, @PathVariable String type,
        @RequestParam(required = false) String state, @RequestParam(required = false) String q) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=objects.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.exportCsv(domain, type, state, q));
    }
}
