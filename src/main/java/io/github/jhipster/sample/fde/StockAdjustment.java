package io.github.jhipster.sample.fde;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Example of the domain-specific portion; the engine contains no stock logic. */
@Component
public class StockAdjustment implements FdeExtension {
    public String id() { return "stock-adjustment"; }
    public Map<String, Object> changes(Map<String, Object> current, Map<String, Object> input) {
        var next = new BigDecimal(current.get("quantity").toString()).add(new BigDecimal(input.get("delta").toString()));
        if (next.signum() < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        return Map.of("quantity", next.longValueExact());
    }
}
