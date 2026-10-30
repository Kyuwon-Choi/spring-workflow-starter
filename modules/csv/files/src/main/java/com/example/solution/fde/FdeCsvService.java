package com.example.solution.fde;

import static com.example.solution.fde.FdeSchema.*;
import static com.example.solution.fde.FdeService.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FdeCsvService {
    public record RowError(long row, String detail) {}
    public record Preview(boolean valid, List<Map<String, Object>> rows, List<RowError> errors) {}
    private final FdeService core;
    private final FdeRegistry registry;
    private final FdeObjectRepository objects;
    public FdeCsvService(FdeService core, FdeRegistry registry, FdeObjectRepository objects) {
        this.core = core; this.registry = registry; this.objects = objects;
    }
    @Transactional(readOnly = true)
    public Preview preview(String domain, String type, String csv) {
        var definition = registry.type(domain, type); core.requireRoles(definition.createRoles());
        if (csv == null || csv.length() > 100000) throw core.bad("CSV is too large");
        var format = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).setAllowMissingColumnNames(false).get();
        var rows = new ArrayList<Map<String, Object>>(); var errors = new ArrayList<RowError>(); var keys = new HashSet<String>();
        try (var parser = format.parse(new StringReader(csv.replaceFirst("^\\uFEFF", "")))) {
            if (parser.getHeaderNames().isEmpty() || !definition.fields().keySet().containsAll(parser.getHeaderNames())) throw core.bad("Unknown CSV columns");
            for (var row : parser) {
                if (row.getRecordNumber() > 100) throw core.bad("Maximum 100 CSV rows per batch");
                try {
                    if (!row.isConsistent()) throw core.bad("Column count does not match header");
                    var clean = core.prepare(domain, type, new LinkedHashMap<>(row.toMap()), true);
                    var key = core.key(definition, clean);
                    var businessScope = core.policy(definition) == null ? "" : core.policy(definition).businessScope(clean);
                    if (key != null && (!keys.add(key) || objects.existsByDomainAndObjectTypeAndBusinessScopeAndBusinessKey(domain, type, businessScope, key))) throw core.bad("Duplicate business key");
                    // Commit revalidates the original client fields, not server-enriched preview data.
                    var client = new LinkedHashMap<>(clean);
                    definition.fields().forEach((name, field) -> { if (field.readOnly()) client.remove(name); });
                    rows.add(client);
                } catch (ResponseStatusException failure) { errors.add(new RowError(row.getRecordNumber() + 1, failure.getReason())); }
            }
        } catch (java.io.IOException | IllegalArgumentException | java.io.UncheckedIOException error) { throw core.bad("Malformed CSV"); }
        if (rows.isEmpty() && errors.isEmpty()) throw core.bad("CSV contains no records");
        return new Preview(errors.isEmpty(), rows, errors);
    }
    public List<View> importRows(String domain, String type, List<Map<String, Object>> rows) {
        core.lockPolicy(registry.type(domain, type));
        if (rows == null || rows.isEmpty() || rows.size() > 100) throw core.bad("Import requires 1 to 100 rows");
        // Validate every row before writes; DB uniqueness and audit failures roll back the entire transaction.
        var prepared = rows.stream().map(row -> core.prepare(domain, type, row, false)).toList();
        return prepared.stream().map(row -> core.persist(domain, type, row)).toList();
    }
    @Transactional(readOnly = true)
    public String exportCsv(String domain, String type, String state, String q) {
        var filter = core.scope(domain, type, state, q);
        if (objects.count(filter) > 1000) throw core.bad("Narrow the filter to at most 1000 export rows");
        var definition = registry.type(domain, type);
        var fields = (core.policy(definition) == null ? definition.fields().keySet() : core.policy(definition).visibleFields(definition, null)).stream().sorted().toList();
        var text = new StringWriter();
        try (var printer = new CSVPrinter(text, CSVFormat.RFC4180.builder().setHeader(fields.toArray(String[]::new)).get())) {
            for (var o : objects.findAll(filter, Sort.by("id"))) {
                var values = core.output(definition, o, core.data(o));
                printer.printRecord(fields.stream().map(name -> safeCsv(values.get(name))).toList());
            }
        } catch (java.io.IOException error) { throw new IllegalStateException("CSV export failed", error); }
        return text.toString();
    }
    private Object safeCsv(Object value) {
        if (value instanceof String text && !text.isEmpty() && ("=+-@\t\r\n".indexOf(text.charAt(0)) >= 0)) return "'" + text;
        return value == null ? "" : value;
    }
}
