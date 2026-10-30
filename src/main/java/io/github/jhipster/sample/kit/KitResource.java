package io.github.jhipster.sample.kit;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kit")
@Validated
@PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
public class KitResource {
    public record NewAsset(@NotBlank @Pattern(regexp = "[A-Z0-9_-]{2,32}") String code,
                           @NotBlank @Size(max = 120) String name) {
        @JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unknown asset field"); }
    }
    public record NewRequest(@NotNull @Positive Long assetId, @NotBlank @Size(max = 160) String title) {
        @JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unknown request field"); }
    }
    public record Transition(@NotNull KitRequest.State target, @NotNull @PositiveOrZero Long expectedVersion) {
        @JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unknown transition field"); }
    }

    private final KitService service;
    public KitResource(KitService service) { this.service = service; }

    @GetMapping("/assets")
    public List<KitService.AssetView> assets() { return service.assets(); }

    @PostMapping("/assets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public KitService.AssetView createAsset(@Valid @RequestBody NewAsset input) {
        return service.createAsset(input.code(), input.name());
    }

    @GetMapping("/requests")
    public List<KitService.RequestView> requests(@RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.requests(page, size);
    }

    @GetMapping("/requests/{id}")
    public KitService.RequestView get(@PathVariable @Positive Long id) { return service.get(id); }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public KitService.RequestView create(@Valid @RequestBody NewRequest input) {
        return service.create(input.assetId(), input.title());
    }

    @PostMapping("/requests/{id}/actions")
    public KitService.RequestView transition(@PathVariable @Positive Long id, @Valid @RequestBody Transition input) {
        return service.transition(id, input.target(), input.expectedVersion());
    }

    @GetMapping("/requests/{id}/history")
    public List<KitService.AuditView> history(@PathVariable @Positive Long id) { return service.history(id); }
}
