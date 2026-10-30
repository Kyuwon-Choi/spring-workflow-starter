package io.github.jhipster.sample.fde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jhipster.sample.IntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@ActiveProfiles("testdev")
@AutoConfigureMockMvc
@WithMockUser(username = "alice", roles = "USER")
class TrainingScenarioTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FdeObjectRepository objects;
    @Autowired FdeAuditRepository audits;
    @BeforeEach void clean() { audits.deleteAllInBatch(); objects.deleteAllInBatch(); }
    private String base(String type) { return "/api/fde/training/" + type; }
    private String json(Object value) { return mapper.writeValueAsString(value); }
    private JsonNode create(String type, Map<String, Object> data, String actor, String role) throws Exception {
        return mapper.readTree(mvc.perform(post(base(type) + "/objects").with(user(actor).roles(role)).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", data)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private long course() throws Exception { return create("course", Map.of("code", "EDU-1", "name", "Quality training", "startDate", "2026-11-01"), "manager", "ADMIN").get("id").asLong(); }
    private JsonNode application(long course, long tuition, String actor) throws Exception {
        return create("application", Map.of("title", "Course request", "course", course, "tuition", tuition), actor, "USER");
    }
    private org.springframework.test.web.servlet.ResultActions action(long id, String action, int version, Map<String, Object> input, String actor, String role) throws Exception {
        return mvc.perform(post(base("application") + "/objects/" + id + "/actions/" + action).with(user(actor).roles(role))
            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("expectedVersion", version, "data", input))));
    }
    @Test void courseRequiresAdminAndUniqueBusinessKey() throws Exception {
        long id = course();
        mvc.perform(get(base("course") + "/objects/" + id)).andExpect(status().isOk());
        var body = json(Map.of("data", Map.of("code", "EDU-1", "name", "Duplicate", "startDate", "2026-11-01")));
        mvc.perform(post(base("course") + "/objects").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post(base("course") + "/objects").with(user("manager").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        assertThat(objects.count()).isEqualTo(1);
    }
    @Test void typedInputAndDateAndReferenceMustBeValid() throws Exception {
        long course = course();
        for (Object invalid : List.of(0, -1, 1000001, 1.5, "100")) {
            mvc.perform(post(base("application") + "/objects").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("data", Map.of("title", "Bad", "course", course, "tuition", invalid))))).andExpect(status().isBadRequest());
        }
        mvc.perform(post(base("course") + "/objects").with(user("manager").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("code", "EDU-2", "name", "Bad date", "startDate", "2026-02-30"))))).andExpect(status().isBadRequest());
        long wrong = create("application", Map.of("title", "Valid", "course", course, "tuition", 1), "alice", "USER").get("id").asLong();
        mvc.perform(post(base("application") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("title", "Wrong relation", "course", wrong, "tuition", 1))))).andExpect(status().isNotFound());
        mvc.perform(post(base("application") + "/objects").contentType(MediaType.APPLICATION_JSON).content("{\"data\":{\"title\":\"Missing\"}}"))
            .andExpect(status().isBadRequest());
    }
    @Test void calculatedAndManagerFieldsCannotBeForgedAtCreationOrImport() throws Exception {
        long course = course();
        for (String field : List.of("grant", "attendanceRate", "reason")) {
            var data = new java.util.HashMap<String, Object>(Map.of("title", "Forged", "course", course, "tuition", 100001));
            data.put(field, field.equals("reason") ? "Forged" : 100);
            mvc.perform(post(base("application") + "/objects").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("data", data))))
                .andExpect(status().isBadRequest());
            mvc.perform(post(base("application") + "/import").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("rows", List.of(data)))))
                .andExpect(status().isBadRequest());
            mvc.perform(post(base("application") + "/import/preview").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("csv", "title,course,tuition," + field + "\nForged," + course + ",100001,100\n"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false));
        }
        assertThat(objects.count()).isEqualTo(1);
        assertThat(audits.count()).isEqualTo(1);
    }
    @Test void approvalBoundaryAndGrantAreComputedOnServerAndFailuresAreAtomic() throws Exception {
        long id = application(course(), 100001, "alice").get("id").asLong();
        action(id, "approve", 0, Map.of("attendanceRate", 80), "manager", "ADMIN").andExpect(status().isConflict());
        action(id, "submit", 0, Map.of(), "alice", "USER").andExpect(status().isOk());
        long count = audits.count();
        action(id, "approve", 1, Map.of("attendanceRate", 80), "alice", "USER").andExpect(status().isForbidden());
        action(id, "approve", 1, Map.of("attendanceRate", 79), "manager", "ADMIN").andExpect(status().isConflict());
        action(id, "approve", 1, Map.of("attendanceRate", 80, "grant", 300000), "manager", "ADMIN").andExpect(status().isBadRequest());
        action(id, "approve", 1, Map.of("attendanceRate", 101), "manager", "ADMIN").andExpect(status().isBadRequest());
        assertThat(audits.count()).isEqualTo(count); assertThat(objects.findById(id).orElseThrow().version).isEqualTo(1);
        action(id, "approve", 1, Map.of("attendanceRate", 80), "manager", "ADMIN").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.grant").value(80000)).andExpect(jsonPath("$.data.attendanceRate").value(80))
            .andExpect(jsonPath("$.state").value("APPROVED")).andExpect(jsonPath("$.version").value(2));
    }
    @Test void grantCapAndSmallAmountUseIntegerFloor() throws Exception {
        long course = course();
        for (long[] example : List.of(new long[]{500000, 300000}, new long[]{1, 0})) {
            long id = application(course, example[0], "alice").get("id").asLong();
            action(id, "submit", 0, Map.of(), "alice", "USER").andExpect(status().isOk());
            action(id, "approve", 1, Map.of("attendanceRate", 100), "manager", "ADMIN").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grant").value(example[1]));
        }
    }
    @Test void rejectionNeedsReasonAndCannotBeRepeated() throws Exception {
        long id = application(course(), 100000, "alice").get("id").asLong();
        action(id, "submit", 0, Map.of(), "alice", "USER").andExpect(status().isOk());
        action(id, "reject", 1, Map.of(), "manager", "ADMIN").andExpect(status().isBadRequest());
        action(id, "reject", 1, Map.of("reason", "Missing evidence"), "manager", "ADMIN").andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("REJECTED")).andExpect(jsonPath("$.data.reason").value("Missing evidence"));
        action(id, "approve", 2, Map.of("attendanceRate", 100), "manager", "ADMIN").andExpect(status().isConflict());
    }
    @Test void allReadAndActionPathsRespectOwnershipWhileAdminSeesBoth() throws Exception {
        long course = course(); long id = application(course, 100000, "alice").get("id").asLong();
        application(course, 200000, "bob");
        for (String path : List.of("/objects/" + id, "/objects/" + id + "/history")) {
            mvc.perform(get(base("application") + path).with(user("bob").roles("USER"))).andExpect(status().isNotFound());
        }
        action(id, "submit", 0, Map.of(), "bob", "USER").andExpect(status().isNotFound());
        mvc.perform(get(base("application") + "/objects").with(user("bob").roles("USER"))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(base("application") + "/summary").with(user("bob").roles("USER"))).andExpect(jsonPath("$.DRAFT").value(1));
        var csv = mvc.perform(get(base("application") + "/export").with(user("bob").roles("USER"))).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("200000").doesNotContain("100000");
        mvc.perform(get(base("application") + "/objects").with(user("manager").roles("ADMIN"))).andExpect(jsonPath("$.total").value(2));
    }
    @Test void staleVersionDoesNotChangeDataOrAudit() throws Exception {
        long id = application(course(), 100000, "alice").get("id").asLong();
        action(id, "submit", 0, Map.of(), "alice", "USER").andExpect(status().isOk());
        long count = audits.count();
        action(id, "approve", 0, Map.of("attendanceRate", 100), "manager", "ADMIN").andExpect(status().isConflict());
        assertThat(objects.findById(id).orElseThrow().state).isEqualTo("PENDING"); assertThat(audits.count()).isEqualTo(count);
    }
    @Test void administratorCannotSubmitAnotherPersonsDraft() throws Exception {
        long id = application(course(), 100001, "alice").get("id").asLong();
        long count = audits.count();
        action(id, "submit", 0, Map.of(), "manager", "ADMIN").andExpect(status().isConflict());
        assertThat(objects.findById(id).orElseThrow().state).isEqualTo("DRAFT"); assertThat(audits.count()).isEqualTo(count);
    }
    @Test void courseCsvPreviewAndBatchAreAtomic() throws Exception {
        var csv = "code,name,startDate\nEDU-A,One,2026-11-01\nEDU-B,Two,2026-11-02\n";
        mvc.perform(post(base("course") + "/import/preview").with(user("manager").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("csv", csv)))).andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
        assertThat(objects.count()).isZero();
        var good = Map.of("code", "EDU-A", "name", "One", "startDate", "2026-11-01");
        var bad = Map.of("code", "EDU-B", "name", "Two", "startDate", "2026-02-30");
        mvc.perform(post(base("course") + "/import").with(user("manager").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("rows", List.of(good, bad))))).andExpect(status().isBadRequest());
        assertThat(objects.count()).isZero(); assertThat(audits.count()).isZero();
        mvc.perform(post(base("course") + "/import").with(user("manager").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("rows", List.of(good, Map.of("code", "EDU-B", "name", "Two", "startDate", "2026-11-02"))))))
            .andExpect(status().isCreated());
        assertThat(objects.count()).isEqualTo(2);
    }
    @Test void filteredExportAndAuditCarryActualStateAndActor() throws Exception {
        long id = application(course(), 100001, "alice").get("id").asLong();
        action(id, "submit", 0, Map.of(), "alice", "USER").andExpect(status().isOk());
        action(id, "approve", 1, Map.of("attendanceRate", 80), "manager", "ADMIN").andExpect(status().isOk());
        mvc.perform(get(base("application") + "/objects?q=Course&state=APPROVED")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(base("application") + "/summary")).andExpect(jsonPath("$.APPROVED").value(1));
        mvc.perform(get(base("application") + "/objects/" + id + "/history"))
            .andExpect(jsonPath("$[0].actor").value("manager")).andExpect(jsonPath("$[0].beforeState").value("PENDING"))
            .andExpect(jsonPath("$[0].afterData.grant").value(80000)).andExpect(jsonPath("$[0].occurredAt").exists());
        var csv = mvc.perform(get(base("application") + "/export?state=APPROVED")).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("80000").contains("100001").doesNotContain("owner");
    }
    @Test void twoConcurrentApprovalsProduceOneChangeAndOneConflict() throws Exception {
        long id = application(course(), 100001, "alice").get("id").asLong();
        action(id, "submit", 0, Map.of(), "alice", "USER").andExpect(status().isOk());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks = List.<java.util.concurrent.Callable<Integer>>of(
                () -> action(id, "approve", 1, Map.of("attendanceRate", 80), "manager", "ADMIN").andReturn().getResponse().getStatus(),
                () -> action(id, "approve", 1, Map.of("attendanceRate", 80), "manager", "ADMIN").andReturn().getResponse().getStatus());
            var statuses = pool.invokeAll(tasks).stream().map(f -> { try { return f.get(); } catch (Exception e) { throw new IllegalStateException(e); } }).sorted().toList();
            assertThat(statuses).containsExactly(200, 409);
        }
        assertThat(audits.findTop200ByObjectIdOrderByIdDesc(id)).hasSize(3);
    }
}
