package io.github.jhipster.sample.fde;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Rehearsal-specific rule; no education logic is added to the shared service. */
@Component
public class TrainingGrant implements FdeExtension {
    public String id() { return "training-grant"; }
    public Map<String, Object> changes(Map<String, Object> current, Map<String, Object> input) {
        long attendance = ((Number) input.get("attendanceRate")).longValue();
        if (attendance < 80) throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance must be at least 80 percent");
        long grant = new BigDecimal(current.get("tuition").toString()).multiply(new BigDecimal("0.8"))
            .min(new BigDecimal("300000")).setScale(0, RoundingMode.DOWN).longValueExact();
        return Map.of("attendanceRate", attendance, "grant", grant);
    }
}
