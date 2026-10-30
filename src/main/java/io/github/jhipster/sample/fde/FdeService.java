package io.github.jhipster.sample.fde;

import static io.github.jhipster.sample.fde.FdeSchema.*;
import io.github.jhipster.sample.security.SecurityUtils;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class FdeService {
    public record View(Long id, String domain, String objectType, Map<String, Object> data, String state,
                       String owner, Long version, Instant createdAt, List<String> availableActions) {}
    public record Page(List<View> items, long total, int page, int size) {}
    public record AuditView(Long id, String actor, String action, Map<String, Object> beforeData,
                            Map<String, Object> afterData, String beforeState, String afterState, Instant occurredAt) {}
    public record RowError(long row, String detail) {}
    public record Preview(boolean valid, List<Map<String, Object>> rows, List<RowError> errors) {}
    private static final TypeReference<Map<String, Object>> DATA = new TypeReference<>() {};
    private final FdeRegistry registry;
    private final FdeObjectRepository objects;
    private final FdeAuditRepository audits;
    private final ObjectMapper mapper;
    private final Map<String, FdeExtension> extensions = new HashMap<>();
    private final Map<String, FdePolicy> policies = new HashMap<>();

    public FdeService(FdeRegistry registry, FdeObjectRepository objects, FdeAuditRepository audits,
                      ObjectMapper mapper, List<FdeExtension> extensions, List<FdePolicy> policies) {
        this.registry = registry; this.objects = objects; this.audits = audits; this.mapper = mapper;
        for (var extension : extensions) {
            if (this.extensions.putIfAbsent(extension.id(), extension) != null) throw new IllegalStateException("Duplicate extension");
        }
        for (var policy : policies) {
            if (this.policies.putIfAbsent(policy.id(), policy) != null) throw new IllegalStateException("Duplicate policy");
        }
        registry.all().forEach(d -> d.types().values().forEach(t -> {
            if (t.policy() != null && !this.policies.containsKey(t.policy())) throw new IllegalStateException("Unknown type policy");
        }));
        registry.all().forEach(d -> d.types().values().forEach(t -> t.actions().values().forEach(a -> {
            if (a.extension() != null && !this.extensions.containsKey(a.extension())) throw new IllegalStateException("Unknown action extension");
        })));
    }
    private FdePolicy policy(ObjectType type) { return type.policy() == null ? null : policies.get(type.policy()); }
    private void lockPolicy(ObjectType type) { if (policy(type) != null) policy(type).lockForWrite(); }
    public List<Domain> catalog() {
        actor();
        return registry.all().stream().map(d -> {
            var types = new LinkedHashMap<String, ObjectType>();
            d.types().forEach((id, type) -> { if (policy(type) == null || policy(type).discoverable()) types.put(id, type); });
            return new Domain(d.id(), d.label(), types);
        }).filter(d -> !d.types().isEmpty()).toList();
    }
    private Map<String, Object> output(ObjectType type, FdeObject object, Map<String, Object> data) {
        if (data == null || policy(type) == null) return data;
        var visible = policy(type).visibleFields(type, object);
        var result = new LinkedHashMap<String, Object>();
        data.forEach((field, value) -> { if (visible.contains(field)) result.put(field, value); });
        return result;
    }
    private ResponseStatusException bad(String detail) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Object or relationship is unavailable"); }
    private String actor() { return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    private boolean roles(List<String> roles) { return roles.stream().anyMatch(SecurityUtils::hasCurrentUserThisAuthority); }
    private boolean manager(ObjectType type) { return roles(type.managerRoles()); }
    private void requireRoles(List<String> roles) {
        actor();
        if (!roles(roles)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role does not permit this operation");
    }
    private boolean owns(FdeObject object) { return object.owner.equals(actor()); }
    private boolean readable(ObjectType type, FdeObject object) {
        return roles(type.readRoles()) && (type.readScope().equals("ALL") || manager(type) || owns(object));
    }
    private FdeObject accessible(String domain, String type, long id, boolean lock) {
        var definition = registry.type(domain, type);
        var object = (lock ? objects.locked(id) : objects.findById(id)).orElseThrow(this::missing);
        if (!object.domain.equals(domain) || !object.objectType.equals(type) || !readable(definition, object)) throw missing();
        if (policy(definition) != null && !objects.exists(policy(definition).scope().and((root, query, cb) -> cb.equal(root.get("id"), id)))) throw missing();
        return object;
    }
    private Map<String, Object> data(FdeObject object) { return mapper.readValue(object.payload, DATA); }
    private View view(FdeObject o) {
        var type = registry.type(o.domain, o.objectType);
        var available = type.actions().entrySet().stream().filter(e -> allowed(type, o, e.getValue()) &&
            (policy(type) == null || policy(type).canAct(o, e.getKey()))).map(Map.Entry::getKey).sorted().toList();
        return new View(o.id, o.domain, o.objectType, output(type, o, data(o)), o.state, o.owner, o.version, o.createdAt, available);
    }
    private boolean allowed(ObjectType type, FdeObject o, Action a) {
        return a.from().contains(o.state) && roles(a.roles()) &&
            (a.scope().equals("ALL") || owns(o) || a.scope().equals("OWN") && manager(type));
    }
    private Specification<FdeObject> scope(String domain, String type, String state, String q) {
        var definition = registry.type(domain, type);
        requireRoles(definition.readRoles());
        if (state != null && !state.isEmpty() && !definition.states().contains(state)) throw bad("Unknown state");
        if (q != null && q.length() > 120) throw bad("Search is too long");
        Specification<FdeObject> base = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("domain"), domain));
            predicates.add(cb.equal(root.get("objectType"), type));
            if (definition.readScope().equals("OWN") && !manager(definition)) predicates.add(cb.equal(root.get("owner"), actor()));
            if (state != null && !state.isEmpty()) predicates.add(cb.equal(root.get("state"), state));
            if (q != null && !q.isBlank()) {
                var escaped = q.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                var searchColumn = policy(definition) == null ? "payload" : "publicSearch";
                predicates.add(cb.like(cb.lower(((org.hibernate.query.criteria.JpaExpression<String>) root.<String>get(searchColumn)).cast(String.class)), "%" + escaped + "%", '\\'));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return policy(definition) == null ? base : base.and(policy(definition).scope());
    }
    @Transactional(readOnly = true)
    public Page list(String domain, String type, String state, String q, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw bad("Invalid page bounds");
        var result = objects.findAll(scope(domain, type, state, q), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new Page(result.map(this::view).getContent(), result.getTotalElements(), page, size);
    }
    @Transactional(readOnly = true)
    public Map<String, Long> summary(String domain, String type) {
        var counts = new LinkedHashMap<String, Long>();
        for (var state : registry.type(domain, type).states()) counts.put(state, objects.count(scope(domain, type, state, null)));
        return counts;
    }
    @Transactional(readOnly = true)
    public View get(String domain, String type, long id) { return view(accessible(domain, type, id, false)); }

    private Map<String, Object> normalize(String domain, Map<String, Field> schema, Map<String, Object> input, boolean csv) {
        if (input == null || input.size() > 40 || !schema.keySet().containsAll(input.keySet())) throw bad("Unknown or protected input field");
        var clean = new LinkedHashMap<String, Object>();
        schema.forEach((name, field) -> {
            Object value = input.get(name);
            if (value == null || value instanceof String s && s.isBlank()) {
                if (field.required()) throw bad(name + ": value is required");
                return;
            }
            try {
                Object normalized = switch (field.type()) {
                    case "string", "enum", "date" -> {
                        if (!(value instanceof String)) throw bad(name + ": expected text");
                        String text = ((String) value).trim();
                        if (text.length() > (field.maxLength() == null ? 500 : field.maxLength())) throw bad(name + ": text is too long");
                        if (field.type().equals("enum") && !field.options().contains(text)) throw bad(name + ": invalid option");
                        if (field.type().equals("date")) LocalDate.parse(text);
                        yield text;
                    }
                    case "integer", "decimal", "reference" -> {
                        if (!(value instanceof Number) && !(csv && value instanceof String)) throw bad(name + ": expected number");
                        var number = new BigDecimal(value.toString());
                        if (number.precision() > 18 || number.scale() > 6) throw bad(name + ": number precision is too large");
                        if (field.min() != null && number.compareTo(field.min()) < 0 || field.max() != null && number.compareTo(field.max()) > 0) throw bad(name + ": outside allowed range");
                        if (field.type().equals("decimal")) yield number;
                        long integer = number.longValueExact();
                        if (field.type().equals("reference")) {
                            if (integer <= 0) throw bad(name + ": invalid reference");
                            accessible(domain, field.reference(), integer, false);
                        }
                        yield integer;
                    }
                    case "boolean" -> {
                        if (value instanceof Boolean) yield value;
                        if (csv && Set.of("true", "false").contains(value.toString())) yield Boolean.valueOf(value.toString());
                        throw bad(name + ": expected boolean");
                    }
                    default -> throw bad(name + ": unsupported field type");
                };
                clean.put(name, normalized);
            } catch (NumberFormatException | ArithmeticException | java.time.DateTimeException error) {
                throw bad(name + ": invalid typed value");
            }
        });
        if (mapper.writeValueAsString(clean).length() > 32000) throw bad("Object is too large");
        return clean;
    }
    private String key(ObjectType type, Map<String, Object> clean) {
        if (type.keyField() == null) return null;
        var key = clean.get(type.keyField()).toString();
        if (key.length() > 255) throw bad("Business key is too long");
        return key;
    }
    private Map<String, Object> prepare(String domain, String type, Map<String, Object> input, boolean csv) {
        var definition = registry.type(domain, type);
        requireRoles(definition.createRoles());
        if (input != null && input.keySet().stream().anyMatch(name -> definition.fields().containsKey(name) && definition.fields().get(name).readOnly())) {
            throw bad("Server-managed fields cannot be supplied at creation");
        }
        var clean = normalize(domain, definition.fields(), input, csv);
        return policy(definition) == null ? clean : normalize(domain, definition.fields(), policy(definition).prepareCreate(clean), false);
    }
    private View persist(String domain, String type, Map<String, Object> clean) {
        var definition = registry.type(domain, type);
        var o = new FdeObject();
        o.domain = domain; o.objectType = type; o.owner = actor(); o.state = definition.initialState();
        o.payload = mapper.writeValueAsString(clean); o.businessKey = key(definition, clean); o.createdAt = Instant.now();
        o.businessScope = policy(definition) == null ? "" : policy(definition).businessScope(clean);
        if (policy(definition) != null) { policy(definition).stamp(o, clean); o.publicSearch = policy(definition).searchText(clean); }
        objects.saveAndFlush(o); audit(o, "create", null, null);
        return view(o);
    }
    public View create(String domain, String type, Map<String, Object> input) {
        lockPolicy(registry.type(domain, type)); return persist(domain, type, prepare(domain, type, input, false));
    }
    private void audit(FdeObject o, String action, String before, String state) {
        var event = new FdeAudit();
        event.object = o; event.actor = actor(); event.action = action; event.beforeData = before; event.beforeState = state;
        event.afterData = o.payload; event.afterState = o.state; event.occurredAt = Instant.now();
        audits.saveAndFlush(event);
    }
    public View action(String domain, String type, long id, String action, Long expectedVersion, Map<String, Object> input) {
        var definition = registry.type(domain, type);
        var command = definition.actions().get(action);
        if (command == null) throw missing();
        requireRoles(command.roles());
        lockPolicy(definition);
        var o = accessible(domain, type, id, true);
        if (!Objects.equals(o.version, expectedVersion)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Object changed; reload before retrying");
        if (!allowed(definition, o, command)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Action is not permitted in the current state or scope");
        if (policy(definition) != null && !policy(definition).canAct(o, action)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Business policy denies this action");
        var parameters = normalize(domain, command.inputs(), input, false);
        var current = data(o);
        for (var field : command.requires()) {
            if (!current.containsKey(field) || current.get(field).toString().isBlank()) throw new ResponseStatusException(HttpStatus.CONFLICT, field + ": prerequisite is missing");
        }
        var next = new LinkedHashMap<>(current);
        command.writes().forEach((field, parameter) -> { if (parameters.containsKey(parameter)) next.put(field, parameters.get(parameter)); });
        String target = command.target();
        if (command.extension() != null) {
            var outcome = extensions.get(command.extension()).apply(Collections.unmodifiableMap(current), Collections.unmodifiableMap(parameters), target);
            if (outcome == null || outcome.changes() == null || !definition.states().contains(outcome.state())) throw new IllegalStateException("Invalid trusted transition");
            next.putAll(outcome.changes()); target = outcome.state();
        }
        var clean = normalize(domain, definition.fields(), next, false);
        var before = o.payload; var previous = o.state;
        o.payload = mapper.writeValueAsString(clean); o.businessKey = key(definition, clean); o.state = target;
        if (policy(definition) != null) o.publicSearch = policy(definition).searchText(clean);
        objects.saveAndFlush(o); audit(o, action, before, previous);
        return view(o);
    }
    @Transactional(readOnly = true)
    public List<AuditView> history(String domain, String type, long id) {
        var object = accessible(domain, type, id, false);
        var definition = registry.type(domain, type);
        return audits.findTop200ByObjectIdOrderByIdDesc(id).stream().map(e -> new AuditView(e.id, e.actor, e.action,
            e.beforeData == null ? null : output(definition, object, mapper.readValue(e.beforeData, DATA)), output(definition, object, mapper.readValue(e.afterData, DATA)), e.beforeState, e.afterState, e.occurredAt)).toList();
    }
    @Transactional(readOnly = true)
    public Preview preview(String domain, String type, String csv) {
        var definition = registry.type(domain, type); requireRoles(definition.createRoles());
        if (csv == null || csv.length() > 100000) throw bad("CSV is too large");
        var format = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).setAllowMissingColumnNames(false).get();
        var rows = new ArrayList<Map<String, Object>>(); var errors = new ArrayList<RowError>(); var keys = new HashSet<String>();
        try (var parser = format.parse(new StringReader(csv.replaceFirst("^\\uFEFF", "")))) {
            if (parser.getHeaderNames().isEmpty() || !definition.fields().keySet().containsAll(parser.getHeaderNames())) throw bad("Unknown CSV columns");
            for (var row : parser) {
                if (row.getRecordNumber() > 100) throw bad("Maximum 100 CSV rows per batch");
                try {
                    if (!row.isConsistent()) throw bad("Column count does not match header");
                    var clean = prepare(domain, type, new LinkedHashMap<>(row.toMap()), true);
                    var key = key(definition, clean);
                    var businessScope = policy(definition) == null ? "" : policy(definition).businessScope(clean);
                    if (key != null && (!keys.add(key) || objects.existsByDomainAndObjectTypeAndBusinessScopeAndBusinessKey(domain, type, businessScope, key))) throw bad("Duplicate business key");
                    // Commit revalidates the original client fields, not server-enriched preview data.
                    var client = new LinkedHashMap<>(clean);
                    definition.fields().forEach((name, field) -> { if (field.readOnly()) client.remove(name); });
                    rows.add(client);
                } catch (ResponseStatusException failure) { errors.add(new RowError(row.getRecordNumber() + 1, failure.getReason())); }
            }
        } catch (java.io.IOException | IllegalArgumentException | java.io.UncheckedIOException error) { throw bad("Malformed CSV"); }
        if (rows.isEmpty() && errors.isEmpty()) throw bad("CSV contains no records");
        return new Preview(errors.isEmpty(), rows, errors);
    }
    public List<View> importRows(String domain, String type, List<Map<String, Object>> rows) {
        lockPolicy(registry.type(domain, type));
        if (rows == null || rows.isEmpty() || rows.size() > 100) throw bad("Import requires 1 to 100 rows");
        // Validate every row before writes; DB uniqueness and audit failures roll back the entire transaction.
        var prepared = rows.stream().map(row -> prepare(domain, type, row, false)).toList();
        return prepared.stream().map(row -> persist(domain, type, row)).toList();
    }
    @Transactional(readOnly = true)
    public String exportCsv(String domain, String type, String state, String q) {
        var filter = scope(domain, type, state, q);
        if (objects.count(filter) > 1000) throw bad("Narrow the filter to at most 1000 export rows");
        var definition = registry.type(domain, type);
        var fields = (policy(definition) == null ? definition.fields().keySet() : policy(definition).visibleFields(definition, null)).stream().sorted().toList();
        var text = new StringWriter();
        try (var printer = new CSVPrinter(text, CSVFormat.RFC4180.builder().setHeader(fields.toArray(String[]::new)).get())) {
            for (var o : objects.findAll(filter, Sort.by("id"))) {
                var values = output(definition, o, data(o));
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
