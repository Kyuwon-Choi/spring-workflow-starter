package io.github.jhipster.sample.fde;

import static io.github.jhipster.sample.fde.FdeSchema.*;
import java.util.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
public class FdeRegistry {
    private final Map<String, Domain> domains = new TreeMap<>();
    public FdeRegistry(ObjectMapper mapper) throws java.io.IOException {
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:fde/domains/*.json");
        for (var resource : resources) {
            try (var stream = resource.getInputStream()) {
                var domain = mapper.readValue(stream, Domain.class);
                validate(domain);
                if (domains.putIfAbsent(domain.id(), domain) != null) throw new IllegalStateException("Duplicate domain");
            }
        }
        if (domains.isEmpty()) throw new IllegalStateException("No domain definitions");
    }
    public List<Domain> all() { return List.copyOf(domains.values()); }
    public Domain domain(String id) {
        var d = domains.get(id);
        if (d == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown domain");
        return d;
    }
    public ObjectType type(String domain, String type) {
        var t = domain(domain).types().get(type);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown object type");
        return t;
    }
    private static void require(boolean valid) {
        if (!valid) throw new IllegalStateException("Invalid trusted FDE definition");
    }
    private static void fields(Map<String, Field> fields) {
        require(fields != null && fields.size() <= 40);
        fields.forEach((name, field) -> {
            require(name.matches("[a-z][a-zA-Z0-9_]{0,39}") && field != null && field.label() != null);
            require(Set.of("string", "integer", "decimal", "boolean", "date", "enum", "reference").contains(field.type()));
            require(!field.readOnly() || !field.required());
            require(field.maxLength() == null || field.maxLength() > 0 && field.maxLength() <= 4000);
            require(field.min() == null || field.max() == null || field.min().compareTo(field.max()) <= 0);
            if (field.type().equals("enum")) require(field.options() != null && !field.options().isEmpty());
        });
    }
    private static void validate(Domain d) {
        require(d.id() != null && d.id().matches("[a-z][a-z0-9-]{0,63}") && d.label() != null && d.types() != null && !d.types().isEmpty());
        d.types().forEach((name, type) -> {
            require(name.matches("[a-z][a-z0-9-]{0,63}") && type.label() != null);
            fields(type.fields());
            require(type.fields().containsKey(type.labelField()));
            require(type.keyField() == null || type.fields().containsKey(type.keyField()) && type.fields().get(type.keyField()).required());
            require(type.keyField() == null || !type.fields().get(type.keyField()).readOnly());
            require(type.states() != null && type.states().contains(type.initialState()));
            require(type.states().stream().allMatch(s -> s.matches("[A-Z][A-Z0-9_]{0,63}")));
            require(type.readRoles() != null && !type.readRoles().isEmpty() && type.createRoles() != null && type.managerRoles() != null);
            require(Set.of("ALL", "OWN").contains(type.readScope()) && type.actions() != null);
            type.fields().values().stream().filter(f -> f.type().equals("reference")).forEach(f -> require(d.types().containsKey(f.reference())));
            type.actions().forEach((id, a) -> {
                require(id.matches("[a-z][a-z0-9-]{0,63}") && a.label() != null && a.from() != null && !a.from().isEmpty());
                require(type.states().containsAll(a.from()) && type.states().contains(a.target()));
                require(a.roles() != null && !a.roles().isEmpty() && Set.of("ALL", "OWN", "OWNER_ONLY").contains(a.scope()));
                fields(a.inputs());
                require(a.inputs().values().stream().noneMatch(Field::readOnly));
                a.inputs().values().stream().filter(f -> f.type().equals("reference")).forEach(f -> require(d.types().containsKey(f.reference())));
                require(a.writes() != null && type.fields().keySet().containsAll(a.writes().keySet()) && a.inputs().keySet().containsAll(a.writes().values()));
                require(a.requires() != null && type.fields().keySet().containsAll(a.requires()));
            });
        });
    }
}
