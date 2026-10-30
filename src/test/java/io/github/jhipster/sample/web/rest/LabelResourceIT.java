package io.github.jhipster.sample.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jhipster.sample.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Original CRUD demonstrations are outside this kit; keep their API boundary closed. */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(username = "user", authorities = "ROLE_USER")
class LabelResourceIT {
    @Autowired MockMvc mvc;
    @Test
    void unusedDemonstrationEndpointsAreDenied() throws Exception {
        mvc.perform(get("/api/labels")).andExpect(status().isForbidden());
        mvc.perform(post("/api/labels")).andExpect(status().isForbidden());
        mvc.perform(put("/api/labels/1")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/labels/1")).andExpect(status().isForbidden());
    }
}
