package com.example.solution.fde;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Trusted, versioned application definitions; never loaded from an untrusted API request. */
public final class FdeSchema {
    private FdeSchema() {}
    public record Domain(String id, String label, Map<String, ObjectType> types) {}
    public record Field(String label, String type, boolean required, Integer maxLength,
                        BigDecimal min, BigDecimal max, List<String> options, String reference, Boolean readOnly) {
        public Field { readOnly = Boolean.TRUE.equals(readOnly); }
    }
    public record ObjectType(String label, String labelField, String keyField, Map<String, Field> fields,
                             List<String> states, String initialState, List<String> readRoles,
                             String readScope, List<String> managerRoles, List<String> createRoles,
                             Map<String, Action> actions, String policy) {}
    public record Action(String label, List<String> from, String target, List<String> roles, String scope,
                         Map<String, Field> inputs, Map<String, String> writes,
                         List<String> requires, String extension) {}
}
