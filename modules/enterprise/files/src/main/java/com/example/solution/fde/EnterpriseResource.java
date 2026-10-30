package com.example.solution.fde;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fde-enterprise")
@PreAuthorize("isAuthenticated()")
public class EnterpriseResource {
    public record Organization(@NotBlank String tenant, @NotBlank String department) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown input"); }
    }
    public record Project(@NotBlank String code, @NotBlank String department, @NotNull @PositiveOrZero Long budgetLimit) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown input"); }
    }
    public record Membership(@NotBlank String login, @NotBlank String department, @NotNull Set<String> roles,
                             @NotNull Set<String> projects, @NotNull Boolean active, Long expectedVersion) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown input"); }
    }
    public record Rule(@NotBlank String key, Instant effectiveAt, @PositiveOrZero Long financeAbove) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown input"); }
    }
    public record Delegation(@NotBlank String to, @NotBlank String project, @NotNull Instant from, @NotNull Instant until) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown input"); }
    }
    private final EnterpriseAdministration service;
    public EnterpriseResource(EnterpriseAdministration service) { this.service = service; }
    @GetMapping public Map<String, Object> overview() { return service.overview(); }
    @PostMapping("/organization") @ResponseStatus(HttpStatus.CREATED)
    public void organization(@Valid @RequestBody Organization input) { service.bootstrap(input.tenant(), input.department()); }
    @PostMapping("/projects") @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseRecords.Project project(@Valid @RequestBody Project input) { return service.project(input.code(), input.department(), input.budgetLimit()); }
    @PostMapping("/members") public EnterpriseRecords.Member member(@Valid @RequestBody Membership input) {
        return service.member(input.login(), input.department(), input.roles(), input.projects(), input.active(), input.expectedVersion());
    }
    @PostMapping("/rules") @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseRecords.Rule rule(@Valid @RequestBody Rule input) { return service.rule(input.key(), input.effectiveAt(), input.financeAbove()); }
    @PostMapping("/delegations") @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseRecords.Delegation delegation(@Valid @RequestBody Delegation input) { return service.delegate(input.to(), input.project(), input.from(), input.until()); }
    @PostMapping("/delegations/{id}/revoke") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable long id) { service.revoke(id); }
}
