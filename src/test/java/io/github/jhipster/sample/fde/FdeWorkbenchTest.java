package io.github.jhipster.sample.fde;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jhipster.sample.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@ActiveProfiles("testdev")
@AutoConfigureMockMvc
@WithMockUser(username = "alice", authorities = "ROLE_USER")
class FdeWorkbenchTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired FdeObjectRepository objects;
    @MockitoSpyBean FdeAuditRepository audits;
    @BeforeEach void clean() { audits.deleteAllInBatch(); objects.deleteAllInBatch(); }
    private String base(String domain, String type) { return "/api/fde/" + domain + "/" + type; }
    private String json(Object data) { return mapper.writeValueAsString(data); }
    private JsonNode create(String domain, String type, Map<String, Object> data) throws Exception {
        return mapper.readTree(mvc.perform(post(base(domain, type) + "/objects").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("data", data))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private long customer() throws Exception { return create("support", "customer", Map.of("code", "C1", "name", "Customer")).get("id").asLong(); }
    private JsonNode ticket(long customer) throws Exception { return create("support", "ticket", Map.of("title", "Help", "customer", customer, "priority", "HIGH")); }
    private JsonNode item() throws Exception { return create("inventory", "item", Map.of("sku", "S1", "name", "Stock", "quantity", 10, "category", "RAW")); }
    private org.springframework.test.web.servlet.ResultActions action(String domain, String type, long id, String action, int version, Map<String, Object> data) throws Exception {
        return mvc.perform(post(base(domain, type) + "/objects/" + id + "/actions/" + action).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("expectedVersion", version, "data", data))));
    }
    @Test @WithAnonymousUser void anonymousIsDenied() throws Exception {
        mvc.perform(get("/api/fde/catalog")).andExpect(status().isUnauthorized());
    }
    @Test void definitionsAreLoadedAndAFourthDomainNeedsOnlyATestResource() throws Exception {
        mvc.perform(get("/api/fde/catalog")).andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItems("support", "procurement", "inventory", "events")));
        var event = create("events", "event", Map.of("title", "Workshop", "capacity", 20, "date", "2026-11-01"));
        action("events", "event", event.get("id").asLong(), "publish", 0, Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PUBLISHED"));
    }
    @Test void actionReferenceParametersUseTheSameRelationshipValidation() throws Exception {
        var first = create("events", "event", Map.of("title", "First", "capacity", 10, "date", "2026-11-01"));
        var second = create("events", "event", Map.of("title", "Second", "capacity", 10, "date", "2026-11-02"));
        action("events", "event", second.get("id").asLong(), "link", 0, Map.of("related", first.get("id").asLong()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.parent").value(first.get("id").asLong()));
        var wrong = item();
        action("events", "event", second.get("id").asLong(), "link", 1, Map.of("related", wrong.get("id").asLong()))
            .andExpect(status().isNotFound());
    }
    @Test void supportRelationshipAndReplyWorkflowUseTheCommonEngine() throws Exception {
        long customer = customer(); var ticket = ticket(customer); long id = ticket.get("id").asLong();
        assertThat(ticket.get("data").get("customer").asLong()).isEqualTo(customer);
        action("support", "ticket", id, "reply", 0, Map.of("response", "Try again")).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("ANSWERED")).andExpect(jsonPath("$.version").value(1));
        action("support", "ticket", id, "close", 1, Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CLOSED"));
        mvc.perform(get(base("support", "ticket") + "/objects/" + id + "/history")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].beforeData.response").value("Try again")).andExpect(jsonPath("$[0].actor").value("alice"));
    }
    @Test void procurementUsesDifferentRolesAndStatesWithoutChangingTheEngine() throws Exception {
        long supplier = create("procurement", "supplier", Map.of("code", "V1", "name", "Supplier")).get("id").asLong();
        long id = create("procurement", "purchase", Map.of("title", "Materials", "supplier", supplier, "amount", 500)).get("id").asLong();
        action("procurement", "purchase", id, "submit", 0, Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"));
        action("procurement", "purchase", id, "approve", 1, Map.of()).andExpect(status().isForbidden());
        mvc.perform(post(base("procurement", "purchase") + "/objects/" + id + "/actions/approve").with(user("manager").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("expectedVersion", 1, "data", Map.of()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("APPROVED"));
    }
    @Test void customStockCalculationRejectsNegativeStockAndRecordsChanges() throws Exception {
        long id = item().get("id").asLong();
        action("inventory", "item", id, "adjust", 0, Map.of("delta", -11)).andExpect(status().isConflict());
        assertThat(objects.findById(id).orElseThrow().version).isZero(); assertThat(audits.count()).isEqualTo(1);
        action("inventory", "item", id, "adjust", 0, Map.of("delta", -3)).andExpect(status().isOk()).andExpect(jsonPath("$.data.quantity").value(7));
    }
    @Test void allScopeCanPermitAnotherUserWhileOwnScopeDeniesThem() throws Exception {
        long stock = item().get("id").asLong();
        mvc.perform(post(base("inventory", "item") + "/objects/" + stock + "/actions/adjust").with(user("bob").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("expectedVersion", 0, "data", Map.of("delta", 1)))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.quantity").value(11));
        long id = ticket(customer()).get("id").asLong();
        mvc.perform(get(base("support", "ticket") + "/objects/" + id).with(user("bob").roles("USER"))).andExpect(status().isNotFound());
        mvc.perform(get(base("support", "ticket") + "/objects/" + id + "/history").with(user("bob").roles("USER"))).andExpect(status().isNotFound());
        mvc.perform(post(base("support", "ticket") + "/objects/" + id + "/actions/reply").with(user("bob").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("expectedVersion", 0, "data", Map.of("response", "Spoof")))))
            .andExpect(status().isNotFound());
        mvc.perform(get(base("support", "ticket") + "/objects").with(user("bob").roles("USER"))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(base("support", "ticket") + "/summary").with(user("bob").roles("USER"))).andExpect(jsonPath("$.OPEN").value(0));
        mvc.perform(get(base("support", "ticket") + "/export").with(user("bob").roles("USER"))).andExpect(content().string(not(containsString("Help"))));
    }
    @Test void staleVersionAndInvalidTransitionDoNotWriteAudit() throws Exception {
        long id = ticket(customer()).get("id").asLong();
        action("support", "ticket", id, "close", 0, Map.of()).andExpect(status().isConflict());
        action("support", "ticket", id, "reply", 0, Map.of("response", "Answer")).andExpect(status().isOk());
        action("support", "ticket", id, "close", 0, Map.of()).andExpect(status().isConflict());
        assertThat(audits.findTop200ByObjectIdOrderByIdDesc(id)).hasSize(2);
    }
    @Test void fieldTypesRequiredEnumsAndUnknownFieldsAreChecked() throws Exception {
        var endpoint = base("inventory", "item") + "/objects";
        for (var bad : java.util.List.of(
            Map.of("sku", "S1", "name", "Stock", "quantity", "10", "category", "RAW"),
            Map.of("sku", "S1", "name", "Stock", "quantity", 1.5, "category", "RAW"),
            Map.of("sku", "S1", "name", "Stock", "quantity", -1, "category", "RAW"),
            Map.of("sku", "S1", "name", "Stock", "quantity", 1, "category", "INVALID"),
            Map.of("sku", "S1", "name", "Stock", "quantity", 1, "category", "RAW", "owner", "admin"),
            Map.of("sku", "S1", "name", " ", "quantity", 1, "category", "RAW"))) {
            mvc.perform(post(endpoint).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("data", bad)))).andExpect(status().isBadRequest());
        }
        assertThat(objects.count()).isZero();
    }
    @Test void envelopeAndActionParametersCannotSetProtectedFields() throws Exception {
        mvc.perform(post(base("inventory", "item") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content("{\"data\":{},\"owner\":\"admin\"}")).andExpect(status().isBadRequest());
        long id = item().get("id").asLong();
        action("inventory", "item", id, "adjust", 0, Map.of("quantity", 999)).andExpect(status().isBadRequest());
    }
    @Test void crossDomainAndWrongTypeRelationshipsAreDenied() throws Exception {
        long item = item().get("id").asLong();
        mvc.perform(post(base("support", "ticket") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("title", "Help", "priority", "HIGH", "customer", item))))).andExpect(status().isNotFound());
        mvc.perform(get(base("support", "customer") + "/objects/" + item)).andExpect(status().isNotFound());
    }
    @Test void dateAndTextLimitsAreCheckedFromDefinitions() throws Exception {
        mvc.perform(post(base("events", "event") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("title", "Event", "capacity", 10, "date", "2026-02-30"))))).andExpect(status().isBadRequest());
        mvc.perform(post(base("support", "customer") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("code", "C1", "name", "x".repeat(101)))))).andExpect(status().isBadRequest());
    }
    @Test void duplicateBusinessKeysAreRejected() throws Exception {
        customer();
        mvc.perform(post(base("support", "customer") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("code", "C1", "name", "Duplicate"))))).andExpect(status().isConflict());
        assertThat(objects.count()).isEqualTo(1);
    }
    @Test void csvPreviewUnderstandsQuotedNewlinesAndTypedValuesWithoutWrites() throws Exception {
        String csv = "sku,name,quantity,category,tracked\r\nS1,\"Name, with comma\",5,RAW,true\r\nS2,\"Two\nlines\",7,FINISHED,false\r\n";
        mvc.perform(post(base("inventory", "item") + "/import/preview").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("csv", csv))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true)).andExpect(jsonPath("$.rows", hasSize(2)))
            .andExpect(jsonPath("$.rows[0].quantity").value(5)).andExpect(jsonPath("$.rows[1].tracked").value(false));
        assertThat(objects.count()).isZero();
    }
    @Test void csvReportsBadRowsAndDuplicateKeysAndRejectsUnknownHeaders() throws Exception {
        mvc.perform(post(base("inventory", "item") + "/import/preview").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("csv", "sku,name,quantity,category\nS1,A,4,RAW\nS1,B,2,RAW\nS3,C,-1,RAW\n"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(false)).andExpect(jsonPath("$.errors", hasSize(2)));
        mvc.perform(post(base("inventory", "item") + "/import/preview").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("csv", "sku,owner\nS1,admin\n")))).andExpect(status().isBadRequest());
    }
    @Test void batchImportIsAtomicOnInvalidRowAndDuplicate() throws Exception {
        var good = Map.of("sku", "S1", "name", "Stock", "quantity", 5, "category", "RAW");
        var bad = Map.of("sku", "S2", "name", "Stock", "quantity", -1, "category", "RAW");
        mvc.perform(post(base("inventory", "item") + "/import").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("rows", java.util.List.of(good, bad))))).andExpect(status().isBadRequest());
        assertThat(objects.count()).isZero();
        mvc.perform(post(base("inventory", "item") + "/import").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("rows", java.util.List.of(good, good))))).andExpect(status().isConflict());
        assertThat(objects.count()).isZero(); assertThat(audits.count()).isZero();
    }
    @Test void successfulBatchImportUsesSameCreatePipeline() throws Exception {
        mvc.perform(post(base("support", "customer") + "/import").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("rows", java.util.List.of(Map.of("code", "C1", "name", "A"), Map.of("code", "C2", "name", "B"))))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)));
        assertThat(objects.count()).isEqualTo(2); assertThat(audits.count()).isEqualTo(2);
    }
    @Test void searchPagingAndSummaryAreBoundedAndReflectStateChanges() throws Exception {
        long customer = customer(); long id = ticket(customer).get("id").asLong();
        action("support", "ticket", id, "reply", 0, Map.of("response", "x".repeat(1500) + "Resolved")).andExpect(status().isOk());
        mvc.perform(get(base("support", "ticket") + "/objects?q=Resolved&state=ANSWERED&size=1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(get(base("support", "ticket") + "/summary")).andExpect(jsonPath("$.ANSWERED").value(1)).andExpect(jsonPath("$.OPEN").value(0));
        mvc.perform(get(base("support", "ticket") + "/objects?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get(base("support", "ticket") + "/objects?state=INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get(base("support", "ticket") + "/objects?q=%25")).andExpect(jsonPath("$.total").value(0));
    }
    @Test void exportedTextIsQuotedAndSpreadsheetFormulasAreNeutralized() throws Exception {
        create("support", "customer", Map.of("code", "C1", "name", "=HYPERLINK(1)"));
        mvc.perform(get(base("support", "customer") + "/export")).andExpect(status().isOk())
            .andExpect(content().string(containsString("'=HYPERLINK(1)"))).andExpect(content().string(not(containsString("owner"))));
    }
    @Test void auditFailureRollsBackCreateAndAction() throws Exception {
        long id = item().get("id").asLong();
        doThrow(new DataIntegrityViolationException("simulated failure")).when(audits).saveAndFlush(any(FdeAudit.class));
        action("inventory", "item", id, "adjust", 0, Map.of("delta", 1)).andExpect(status().isConflict());
        assertThat(objects.findById(id).orElseThrow().version).isZero();
        assertThat(mapper.readTree(objects.findById(id).orElseThrow().payload).get("quantity").asLong()).isEqualTo(10);
        mvc.perform(post(base("support", "customer") + "/objects").contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("data", Map.of("code", "C1", "name", "A"))))).andExpect(status().isConflict());
        assertThat(objects.count()).isEqualTo(1); assertThat(audits.count()).isEqualTo(1);
    }
    @Test @WithMockUser(username = "viewer", authorities = "ROLE_VIEWER") void unrelatedRoleCannotReadOrCreateObjects() throws Exception {
        mvc.perform(get(base("inventory", "item") + "/objects")).andExpect(status().isForbidden());
        mvc.perform(post(base("inventory", "item") + "/objects").contentType(MediaType.APPLICATION_JSON).content("{\"data\":{}}"))
            .andExpect(status().isForbidden());
    }
}
