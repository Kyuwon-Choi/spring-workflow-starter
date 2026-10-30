package io.github.jhipster.sample.fde;

import java.util.Map;

/** A trusted domain-specific calculation. No expression evaluation or external code loading. */
public interface FdeExtension {
    record Outcome(Map<String, Object> changes, String state) {}
    String id();
    Map<String, Object> changes(Map<String, Object> current, Map<String, Object> input);
    default Outcome apply(Map<String, Object> current, Map<String, Object> input, String target) {
        return new Outcome(changes(current, input), target);
    }
}
