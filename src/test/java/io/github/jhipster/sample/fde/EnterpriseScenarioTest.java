package io.github.jhipster.sample.fde;

import static io.github.jhipster.sample.fde.EnterpriseRecords.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jhipster.sample.IntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest @ActiveProfiles("testdev") @AutoConfigureMockMvc
class EnterpriseScenarioTest {
    static final String BASE = "/api/fde/enterprise/request";
    static final String ADMIN = "/api/fde-enterprise";
    static final Instant START = Instant.parse("2026-10-28T00:00:00Z");
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EnterpriseStore store;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    @Autowired FdeObjectRepository objects;
    @MockitoSpyBean FdeAuditRepository audits;
    @MockitoBean Clock clock;
    @Autowired PlatformTransactionManager manager;
    void tx(Runnable task) { new TransactionTemplate(manager).executeWithoutResult(status -> task.run()); }
    void clear() { tx(() -> {
        audits.deleteAllInBatch(); objects.deleteAllInBatch();
        for (String name : List.of("EnterpriseConfigAudit", "EnterpriseDelegation", "EnterpriseRule", "EnterpriseProject", "EnterpriseMember", "EnterpriseTenant")) em.createQuery("delete from " + name).executeUpdate();
    }); }
    void member(String name, String tenant, String department, String roles, String projects) {
        var m = new Member(); m.login = name; m.tenant = tenant; m.department = department; m.roles = roles; m.projects = projects; store.save(m);
    }
    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(START); clear();
        tx(() -> {
            for (var tenant : List.of("T1", "T2")) { var t = new Tenant(); t.id = tenant; store.save(t); }
            for (var values : List.of(List.of("T1", "P1", "D1"), List.of("T1", "P2", "D2"), List.of("T2", "P1", "D1"))) {
                var p = new Project(); p.tenant = values.get(0); p.code = values.get(1); p.department = values.get(2); p.budgetLimit = 10000000; store.save(p);
            }
            for (var tenant : List.of("T1", "T2")) { var r = new Rule(); r.tenant = tenant; r.key = "V1"; r.effectiveAt = Instant.EPOCH; store.save(r); }
            member("alice", "T1", "D1", "STAFF", "P1"); member("bob", "T1", "D2", "STAFF", "P2");
            member("boss", "T1", "D1", "MANAGER,ORG_ADMIN", "P1,P2"); member("finance", "T1", "D2", "FINANCE", "P1,P2");
            member("delegate", "T1", "D1", "STAFF", "P1"); member("outsider", "T2", "D1", "MANAGER,ORG_ADMIN", "P1");
        });
    }
    @AfterEach void cleanup() { clear(); }
    String json(Object value) { return mapper.writeValueAsString(value); }
    org.springframework.test.web.servlet.ResultActions post(String path, Object data, String actor) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).with(user(actor).roles("USER", "ADMIN")).contentType(MediaType.APPLICATION_JSON).content(json(data)));
    }
    JsonNode create(String actor, String project, int amount) throws Exception {
        return mapper.readTree(post(BASE + "/objects", Map.of("data", Map.of("title", "Request " + actor, "project", project, "amount", amount)), actor)
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    org.springframework.test.web.servlet.ResultActions act(JsonNode object, String action, Map<String, Object> data, String actor) throws Exception {
        return post(BASE + "/objects/" + object.get("id").asLong() + "/actions/" + action,
            Map.of("expectedVersion", object.get("version").asLong(), "data", data), actor);
    }
    JsonNode submit(JsonNode object, String actor) throws Exception {
        return mapper.readTree(act(object, "submit", Map.of(), actor).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    JsonNode approve(JsonNode object, String action, String actor) throws Exception {
        return mapper.readTree(act(object, action, Map.of(), actor).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    String get(String path, String actor, int status) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).with(user(actor).roles("USER", "ADMIN"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status)).andReturn().getResponse().getContentAsString();
    }
    void publish(Instant at) throws Exception {
        post(ADMIN + "/rules", Map.of("key", "V2", "effectiveAt", at.toString(), "financeAbove", 300000), "boss").andExpect(status().isCreated());
    }
    double spent() { final long[] value = {0}; tx(() -> value[0] = store.project("T1", "P1").orElseThrow().spent); return value[0]; }
    @Test void tenantDepartmentAndProjectAreEnforcedOnEveryReadSurface() throws Exception {
        var a = create("alice", "P1", 100); create("bob", "P2", 200); create("outsider", "P1", 300);
        for (var actor : List.of("bob", "outsider")) {
            get(BASE + "/objects/" + a.get("id").asLong(), actor, 404);
            get(BASE + "/objects/" + a.get("id").asLong() + "/history", actor, 404);
            act(a, "submit", Map.of(), actor).andExpect(status().isNotFound());
        }
        assertThat(mapper.readTree(get(BASE + "/objects?size=1", "boss", 200)).get("total").asInt()).isEqualTo(1);
        assertThat(mapper.readTree(get(BASE + "/summary", "boss", 200)).get("DRAFT").asInt()).isEqualTo(1);
        assertThat(get(BASE + "/export", "boss", 200)).contains("100").doesNotContain("Request bob").doesNotContain("Request outsider");
        assertThat(mapper.readTree(get(BASE + "/objects", "finance", 200)).get("total").asInt()).isEqualTo(2);
    }
    @Test void clientCannotForgeTenantOrCreateOutsideCurrentProject() throws Exception {
        post(BASE + "/objects", Map.of("data", Map.of("title", "Forged", "project", "P1", "amount", 100, "tenant", "T2")), "alice").andExpect(status().isBadRequest());
        post(BASE + "/objects", Map.of("data", Map.of("title", "Wrong project", "project", "P2", "amount", 100)), "alice").andExpect(status().isForbidden());
        get(ADMIN, "unassigned", 403);
    }
    @Test void administratorsCannotSubmitOrApproveTheirOwnOrOtherPersonsDraft() throws Exception {
        var alice = create("alice", "P1", 100); act(alice, "submit", Map.of(), "boss").andExpect(status().isConflict());
        var own = submit(create("boss", "P1", 100), "boss");
        act(own, "approve-department", Map.of(), "boss").andExpect(status().isForbidden());
        assertThat(spent()).isZero();
    }
    @Test void oldSubmittedPolicySurvivesNewRuleAndNewHighAmountNeedsTwoApprovals() throws Exception {
        var old = submit(create("alice", "P1", 400000), "alice");
        publish(START.plusSeconds(10)); when(clock.instant()).thenReturn(START.plusSeconds(10));
        var fresh = submit(create("alice", "P1", 400000), "alice");
        assertThat(old.get("data").get("policyVersion").asText()).isEqualTo("V1");
        assertThat(fresh.get("data").get("policyVersion").asText()).isEqualTo("V2");
        assertThat(approve(old, "approve-department", "boss").get("state").asText()).isEqualTo("APPROVED");
        var reviewed = approve(fresh, "approve-department", "boss"); assertThat(reviewed.get("state").asText()).isEqualTo("FINANCE_PENDING");
        assertThat(spent()).isEqualTo(400000);
        assertThat(approve(reviewed, "approve-finance", "finance").get("state").asText()).isEqualTo("APPROVED");
        assertThat(spent()).isEqualTo(800000);
    }
    @Test void policyTimeAndAmountBoundariesAreExactAndVersionsCannotBeOverwritten() throws Exception {
        publish(START.plusSeconds(10));
        when(clock.instant()).thenReturn(START.plusSeconds(9));
        assertThat(submit(create("alice", "P1", 300001), "alice").get("data").get("policyVersion").asText()).isEqualTo("V1");
        when(clock.instant()).thenReturn(START.plusSeconds(10));
        var exact = submit(create("alice", "P1", 300000), "alice");
        assertThat(approve(exact, "approve-department", "boss").get("state").asText()).isEqualTo("APPROVED");
        var high = submit(create("alice", "P1", 300001), "alice");
        act(high, "approve-finance", Map.of(), "finance").andExpect(status().isConflict());
        assertThat(approve(high, "approve-department", "boss").get("state").asText()).isEqualTo("FINANCE_PENDING");
        post(ADMIN + "/rules", Map.of("key", "V2", "financeAbove", 0), "boss").andExpect(status().isConflict());
        post(ADMIN + "/rules", Map.of("key", "V3", "effectiveAt", START.toString()), "boss").andExpect(status().isBadRequest());
    }
    @Test void rejectedResubmissionChoosesNewVersionAndRetainsOldAudit() throws Exception {
        var first = submit(create("alice", "P1", 400000), "alice");
        var rejected = mapper.readTree(act(first, "reject", Map.of("reason", "Need evidence"), "boss").andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        publish(START.plusSeconds(1)); when(clock.instant()).thenReturn(START.plusSeconds(2));
        var again = submit(rejected, "alice");
        assertThat(again.get("data").get("policyVersion").asText()).isEqualTo("V2"); assertThat(again.get("data").has("reason")).isFalse();
        assertThat(get(BASE + "/objects/" + again.get("id").asLong() + "/history", "alice", 200)).contains("Need evidence").contains("V1");
    }
    JsonNode delegate(Instant from, Instant until) throws Exception {
        return mapper.readTree(post(ADMIN + "/delegations", Map.of("to", "delegate", "project", "P1", "from", from.toString(), "until", until.toString()), "boss")
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @Test void delegationStartsInclusivelyEndsExclusivelyAndCannotApproveGrantorsOwn() throws Exception {
        var request = submit(create("alice", "P1", 100), "alice");
        delegate(START.plusSeconds(10), START.plusSeconds(20));
        get(BASE + "/objects/" + request.get("id").asLong(), "delegate", 404);
        when(clock.instant()).thenReturn(START.plusSeconds(10));
        assertThat(mapper.readTree(get(BASE + "/objects/" + request.get("id").asLong(), "delegate", 200)).get("availableActions").toString()).contains("approve-department");
        var own = submit(create("boss", "P1", 100), "boss"); act(own, "approve-department", Map.of(), "delegate").andExpect(status().isForbidden());
        when(clock.instant()).thenReturn(START.plusSeconds(20)); act(request, "approve-department", Map.of(), "delegate").andExpect(status().isNotFound());
    }
    @Test void revokedDelegationAndChangedMembershipImmediatelyInvalidateOldScreens() throws Exception {
        var request = submit(create("alice", "P1", 100), "alice"); var delegation = delegate(START, START.plusSeconds(3600));
        get(BASE + "/objects/" + request.get("id").asLong(), "delegate", 200);
        post(ADMIN + "/delegations/" + delegation.get("id").asLong() + "/revoke", Map.of(), "boss").andExpect(status().isNoContent());
        act(request, "approve-department", Map.of(), "delegate").andExpect(status().isNotFound());
        post(ADMIN + "/members", Map.of("login", "boss", "department", "D2", "roles", Set.of("MANAGER", "ORG_ADMIN"), "projects", Set.of("P1", "P2"), "active", true, "expectedVersion", 0), "boss").andExpect(status().isOk());
        act(request, "approve-department", Map.of(), "boss").andExpect(status().isNotFound()); assertThat(spent()).isZero();
    }
    @Test void fieldRedactionAlsoAppliesToHistoryExportAndSearchCounts() throws Exception {
        var request = submit(create("alice", "P1", 100), "alice");
        act(request, "approve-department", Map.of("note", "CONFIDENTIAL-ZEBRA"), "boss").andExpect(status().isOk());
        var id = request.get("id").asLong();
        for (String path : List.of("/objects/" + id, "/objects/" + id + "/history", "/export")) assertThat(get(BASE + path, "alice", 200)).doesNotContain("CONFIDENTIAL-ZEBRA").doesNotContain("internalNote");
        assertThat(mapper.readTree(get(BASE + "/objects?q=CONFIDENTIAL-ZEBRA", "alice", 200)).get("total").asInt()).isZero();
        assertThat(get(BASE + "/objects/" + id, "boss", 200)).contains("CONFIDENTIAL-ZEBRA");
    }
    @Test void onePersonsTwoRolesCannotSupplyBothApprovals() throws Exception {
        tx(() -> { store.member("boss").orElseThrow().roles = "MANAGER,FINANCE,ORG_ADMIN"; });
        publish(START); var request = submit(create("alice", "P1", 400000), "alice");
        var pending = approve(request, "approve-department", "boss");
        act(pending, "approve-finance", Map.of(), "boss").andExpect(status().isForbidden());
        approve(pending, "approve-finance", "finance");
    }
    @Test void simultaneousDifferentRequestsCannotOverspendSharedBudget() throws Exception {
        tx(() -> store.project("T1", "P1").orElseThrow().budgetLimit = 100);
        var a = submit(create("alice", "P1", 70), "alice"); var b = submit(create("delegate", "P1", 70), "delegate");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var futures = pool.invokeAll(List.<java.util.concurrent.Callable<Integer>>of(
                () -> act(a, "approve-department", Map.of(), "boss").andReturn().getResponse().getStatus(),
                () -> act(b, "approve-department", Map.of(), "boss").andReturn().getResponse().getStatus()));
            var values = futures.stream().map(f -> { try { return f.get(); } catch (Exception e) { throw new IllegalStateException(e); } }).sorted().toList();
            assertThat(values).containsExactly(200, 409);
        }
        assertThat(spent()).isEqualTo(70);
    }
    @Test void auditFailureRollsBackBudgetRequestAndRetryDoesNotDoubleSpend() throws Exception {
        var request = submit(create("alice", "P1", 100), "alice");
        doThrow(new DataIntegrityViolationException("simulated audit failure")).when(audits).saveAndFlush(any(FdeAudit.class));
        act(request, "approve-department", Map.of(), "boss").andExpect(status().isConflict());
        assertThat(spent()).isZero(); assertThat(mapper.readTree(get(BASE + "/objects/" + request.get("id").asLong(), "alice", 200)).get("state").asText()).isEqualTo("SUBMITTED");
        reset(audits);
        var approved = approve(request, "approve-department", "boss");
        act(approved, "approve-department", Map.of(), "boss").andExpect(status().isConflict()); assertThat(spent()).isEqualTo(100);
    }
    @Test void membershipChangesAreScopedVersionedAndAudited() throws Exception {
        var body = Map.of("login", "alice", "department", "D1", "roles", Set.of("STAFF"), "projects", Set.of("P1"), "active", false, "expectedVersion", 0);
        post(ADMIN + "/members", body, "alice").andExpect(status().isForbidden());
        post(ADMIN + "/members", body, "outsider").andExpect(status().isNotFound());
        post(ADMIN + "/members", body, "boss").andExpect(status().isOk());
        get(BASE + "/objects", "alice", 403);
        post(ADMIN + "/members", body, "boss").andExpect(status().isConflict());
        assertThat(get(ADMIN, "boss", 200)).contains("membership-change").contains("beforeData");
    }
    @Test void projectRevocationAndGrantorRoleRemovalInvalidateDelegation() throws Exception {
        var request = submit(create("alice", "P1", 100), "alice"); delegate(START, START.plusSeconds(3600));
        tx(() -> store.member("boss").orElseThrow().roles = "ORG_ADMIN");
        act(request, "approve-department", Map.of(), "delegate").andExpect(status().isNotFound());
        tx(() -> { store.member("boss").orElseThrow().roles = "MANAGER,ORG_ADMIN"; store.member("delegate").orElseThrow().projects = ""; });
        act(request, "approve-department", Map.of(), "delegate").andExpect(status().isNotFound());
    }
    @Test void csvPreviewAndImportUseTrustedOrganizationAndAreAtomic() throws Exception {
        post(BASE + "/import/preview", Map.of("csv", "title,project,amount\nOne,P1,100\n"), "alice").andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.rows[0].tenant").doesNotExist());
        assertThat(objects.count()).isZero();
        post(BASE + "/import", Map.of("rows", List.of(Map.of("title", "One", "project", "P1", "amount", 100), Map.of("title", "Bad", "project", "P2", "amount", 100))), "alice")
            .andExpect(status().isForbidden()); assertThat(objects.count()).isZero();
        post(BASE + "/import", Map.of("rows", List.of(Map.of("title", "One", "project", "P1", "amount", 100))), "alice")
            .andExpect(status().isCreated()).andExpect(jsonPath("$[0].data.tenant").value("T1"));
    }
    @Test void aDifferentDomainReusesThePolicyWithCompanyScopedBusinessKeys() throws Exception {
        String base = "/api/fde/tenant-key-test/request";
        var data = Map.of("data", Map.of("title", "SAME-CODE", "project", "P1", "amount", 100));
        post(base + "/objects", data, "alice").andExpect(status().isCreated());
        post(base + "/objects", data, "outsider").andExpect(status().isCreated());
        post(base + "/objects", data, "alice").andExpect(status().isConflict());
        assertThat(mapper.readTree(get(base + "/objects", "alice", 200)).get("total").asInt()).isEqualTo(1);
    }
}
