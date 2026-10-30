package io.github.jhipster.sample.fde;

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
public class FdeResource {
    public record Create(@NotNull Map<String, Object> data) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown envelope field"); }
    }
    public record Command(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Map<String, Object> data) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown envelope field"); }
    }
    public record Csv(@NotBlank @Size(max = 100000) String csv) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown envelope field"); }
    }
    public record Batch(@NotNull @Size(min = 1, max = 100) List<Map<String, Object>> rows) {
        @JsonAnySetter public void unknown(String field, Object value) { throw new IllegalArgumentException("Unknown envelope field"); }
    }
    private final FdeService service;
    public FdeResource(FdeService service) { this.service = service; }
    @GetMapping("/catalog") public List<FdeSchema.Domain> catalog() { return service.catalog(); }
    @GetMapping("/{domain}/{type}/objects")
    public FdeService.Page list(@PathVariable String domain, @PathVariable String type,
        @RequestParam(required = false) String state, @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return service.list(domain, type, state, q, page, size);
    }
    @GetMapping("/{domain}/{type}/summary")
    public Map<String, Long> summary(@PathVariable String domain, @PathVariable String type) { return service.summary(domain, type); }
    @GetMapping("/{domain}/{type}/objects/{id}")
    public FdeService.View get(@PathVariable String domain, @PathVariable String type, @PathVariable @Positive long id) { return service.get(domain, type, id); }
    @PostMapping("/{domain}/{type}/objects") @ResponseStatus(HttpStatus.CREATED)
    public FdeService.View create(@PathVariable String domain, @PathVariable String type, @Valid @RequestBody Create input) {
        return service.create(domain, type, input.data());
    }
    @PostMapping("/{domain}/{type}/objects/{id}/actions/{action}")
    public FdeService.View action(@PathVariable String domain, @PathVariable String type, @PathVariable @Positive long id,
        @PathVariable String action, @Valid @RequestBody Command input) {
        return service.action(domain, type, id, action, input.expectedVersion(), input.data());
    }
    @GetMapping("/{domain}/{type}/objects/{id}/history")
    public List<FdeService.AuditView> history(@PathVariable String domain, @PathVariable String type, @PathVariable @Positive long id) { return service.history(domain, type, id); }
    @PostMapping("/{domain}/{type}/import/preview")
    public FdeService.Preview preview(@PathVariable String domain, @PathVariable String type, @Valid @RequestBody Csv input) { return service.preview(domain, type, input.csv()); }
    @PostMapping("/{domain}/{type}/import") @ResponseStatus(HttpStatus.CREATED)
    public List<FdeService.View> importRows(@PathVariable String domain, @PathVariable String type, @Valid @RequestBody Batch input) { return service.importRows(domain, type, input.rows()); }
    @GetMapping(value = "/{domain}/{type}/export", produces = "text/csv")
    public ResponseEntity<String> export(@PathVariable String domain, @PathVariable String type,
        @RequestParam(required = false) String state, @RequestParam(required = false) String q) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=objects.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.exportCsv(domain, type, state, q));
    }
}
