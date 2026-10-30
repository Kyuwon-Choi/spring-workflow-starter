package io.github.jhipster.sample.kit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jhipster.sample.IntegrationTest;
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
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@ActiveProfiles("testdev")
@AutoConfigureMockMvc
@WithMockUser(username = "alice", authorities = "ROLE_USER")
class KitWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired KitAssetRepository assets;
    @Autowired KitRequestRepository requests;
    @MockitoSpyBean KitAuditRepository events;
    private Long assetId;

    @BeforeEach
    void prepare() {
        events.deleteAllInBatch();
        requests.deleteAllInBatch();
        assets.deleteAllInBatch();
        assetId = assets.saveAndFlush(new KitAsset("EQ-01", "Test equipment")).id;
    }

    private String input(String title) {
        return "{\"assetId\":" + assetId + ",\"title\":" + mapper.writeValueAsString(title) + "}";
    }

    private Long create() throws Exception {
        var response = mvc.perform(post("/api/kit/requests").contentType(MediaType.APPLICATION_JSON).content(input("Inspect sensor")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerLogin").value("alice"))
            .andExpect(jsonPath("$.state").value("OPEN"))
            .andExpect(jsonPath("$.assetCode").value("EQ-01"))
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }

    @Test
    @WithAnonymousUser
    void anonymousAccessIsDenied() throws Exception {
        mvc.perform(get("/api/kit/requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedCreationRecordsActorAndRelationship() throws Exception {
        var id = create();
        mvc.perform(get("/api/kit/requests/{id}", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assetId").value(assetId))
            .andExpect(jsonPath("$.assetCode").value("EQ-01"));
        mvc.perform(get("/api/kit/requests"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].assetCode").value("EQ-01"));
        mvc.perform(get("/api/kit/requests/{id}/history", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].actor").value("alice"))
            .andExpect(jsonPath("$[0].action").value("CREATED"));
    }

    @Test
    void anotherUserCannotReadChangeOrReadHistory() throws Exception {
        var id = create();
        mvc.perform(get("/api/kit/requests/{id}", id).with(user("bob").roles("USER"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/kit/requests/{id}/history", id).with(user("bob").roles("USER"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/kit/requests/{id}/actions", id).with(user("bob").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"IN_PROGRESS\",\"expectedVersion\":0}"))
            .andExpect(status().isNotFound());
        assertThat(requests.findById(id).orElseThrow().state).isEqualTo(KitRequest.State.OPEN);
        assertThat(events.count()).isEqualTo(1);
    }

    @Test
    void listingIsScopedAndAdminCanSeeAll() throws Exception {
        create();
        mvc.perform(post("/api/kit/requests").with(user("bob").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON).content(input("Bob request"))).andExpect(status().isCreated());
        mvc.perform(get("/api/kit/requests")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/kit/requests").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void administratorCanAdvanceAnotherUsersRequest() throws Exception {
        var id = create();
        mvc.perform(post("/api/kit/requests/{id}/actions", id).with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"target\":\"IN_PROGRESS\",\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("IN_PROGRESS"));
        mvc.perform(get("/api/kit/requests/{id}/history", id)).andExpect(jsonPath("$[1].actor").value("admin"));
    }

    @Test
    @WithMockUser(username = "viewer", authorities = "ROLE_VIEWER")
    void unrelatedRoleCannotUseModule() throws Exception {
        mvc.perform(get("/api/kit/requests")).andExpect(status().isForbidden());
    }

    @Test
    void assetCreationRequiresAdminAndDuplicateCodeIsConflict() throws Exception {
        var payload = "{\"code\":\"EQ-02\",\"name\":\"Second equipment\"}";
        mvc.perform(post("/api/kit/assets").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(post("/api/kit/assets").with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mvc.perform(post("/api/kit/assets").with(user("admin").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
    }

    @Test
    void invalidInputsDoNotCreateRequests() throws Exception {
        mvc.perform(post("/api/kit/requests").contentType(MediaType.APPLICATION_JSON).content(input("   "))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/kit/requests").contentType(MediaType.APPLICATION_JSON).content(input("x".repeat(161)))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/kit/requests").contentType(MediaType.APPLICATION_JSON)
            .content("{\"assetId\":999999,\"title\":\"Missing equipment\"}")).andExpect(status().isNotFound());
        assertThat(requests.count()).isZero();
    }

    @Test
    void protectedFieldsCannotBeAssignedByClient() throws Exception {
        mvc.perform(post("/api/kit/requests").contentType(MediaType.APPLICATION_JSON)
            .content("{\"assetId\":" + assetId + ",\"title\":\"Spoofed\",\"ownerLogin\":\"bob\",\"state\":\"DONE\"}"))
            .andExpect(status().isBadRequest());
        assertThat(requests.count()).isZero();
    }

    @Test
    void allowedWorkflowHasAtomicHistoryAndVersion() throws Exception {
        var id = create();
        mvc.perform(post("/api/kit/requests/{id}/actions", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"target\":\"IN_PROGRESS\",\"expectedVersion\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(post("/api/kit/requests/{id}/actions", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"target\":\"DONE\",\"expectedVersion\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("DONE")).andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/api/kit/requests/{id}/history", id)).andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[2].fromState").value("IN_PROGRESS")).andExpect(jsonPath("$[2].toState").value("DONE"));
    }

    @Test
    void skippingStateAndReusingStaleVersionDoNotMutateData() throws Exception {
        var id = create();
        mvc.perform(post("/api/kit/requests/{id}/actions", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"target\":\"DONE\",\"expectedVersion\":0}")).andExpect(status().isConflict());
        mvc.perform(post("/api/kit/requests/{id}/actions", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"target\":\"IN_PROGRESS\",\"expectedVersion\":0}")).andExpect(status().isOk());
        mvc.perform(post("/api/kit/requests/{id}/actions", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"target\":\"DONE\",\"expectedVersion\":0}")).andExpect(status().isConflict());
        assertThat(requests.findById(id).orElseThrow().state).isEqualTo(KitRequest.State.IN_PROGRESS);
        assertThat(events.count()).isEqualTo(2);
    }

    @Test
    void failedHistoryWriteRollsBackCreation() throws Exception {
        doThrow(new DataIntegrityViolationException("Simulated audit failure")).when(events).saveAndFlush(any(KitAuditEvent.class));
        mvc.perform(post("/api/kit/requests").contentType(MediaType.APPLICATION_JSON).content(input("Rollback me")))
            .andExpect(status().isConflict());
        assertThat(requests.count()).isZero();
        assertThat(events.count()).isZero();
    }

    @Test
    void pageBoundsAreValidated() throws Exception {
        mvc.perform(get("/api/kit/requests?size=1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/kit/requests?page=-1")).andExpect(status().isBadRequest());
    }

    @Test
    void failedHistoryWriteAlsoRollsBackTransition() throws Exception {
        var id = create();
        doThrow(new DataIntegrityViolationException("Simulated audit failure")).when(events).saveAndFlush(any(KitAuditEvent.class));
        mvc.perform(post("/api/kit/requests/{id}/actions", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"target\":\"IN_PROGRESS\",\"expectedVersion\":0}"))
            .andExpect(status().isConflict());
        var unchanged = requests.findById(id).orElseThrow();
        assertThat(unchanged.state).isEqualTo(KitRequest.State.OPEN);
        assertThat(unchanged.version).isZero();
        assertThat(events.count()).isEqualTo(1);
    }

    @Test
    void unusedSampleApisAreClosed() throws Exception {
        mvc.perform(get("/api/bank-accounts")).andExpect(status().isForbidden());
        mvc.perform(get("/api/labels")).andExpect(status().isForbidden());
        mvc.perform(get("/api/authorities")).andExpect(status().isForbidden());
    }
}
