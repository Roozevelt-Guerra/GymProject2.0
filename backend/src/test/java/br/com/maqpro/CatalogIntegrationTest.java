package br.com.maqpro;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"app.admin-password=test-password-123", "app.whatsapp=5585999999999"})
@AutoConfigureMockMvc
class CatalogIntegrationTest {
  @org.junit.jupiter.api.io.TempDir static java.nio.file.Path directory;

  @org.springframework.test.context.DynamicPropertySource
  static void storage(org.springframework.test.context.DynamicPropertyRegistry registry) {
    registry.add("app.catalog-file", () -> directory.resolve("equipment.json").toString());
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  String payload =
      """
      {"name":"Leg press","description":"Estrutura profissional","price":12000.50,"imageUrl":"https://example.com/leg.jpg","category":"Musculação"}
      """;

  @Test
  void publicCatalogAndProtectedMutations() throws Exception {
    mvc.perform(get("/api/equipment")).andExpect(status().isOk());
    mvc.perform(get("/api/config")).andExpect(jsonPath("$.whatsappNumber").value("5585999999999"));
    mvc.perform(
            post("/api/equipment").with(csrf()).contentType("application/json").content(payload))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/equipment")
                .with(user("admin").roles("ADMIN"))
                .contentType("application/json")
                .content(payload))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCrudAndValidation() throws Exception {
    String json =
        mvc.perform(
                post("/api/equipment")
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(payload))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = mapper.readTree(json).get("id").asLong();
    mvc.perform(get("/api/equipment/" + id)).andExpect(jsonPath("$.price").value(12000.50));
    mvc.perform(
            put("/api/equipment/" + id)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(payload.replace("12000.50", "9000.25")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price").value(9000.25));
    mvc.perform(
            post("/api/equipment")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(payload.replace("12000.50", "-1")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/equipment")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(payload.replace("https://example.com/leg.jpg", "javascript:alert(1)")))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/equipment/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/equipment/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void sessionLoginAndLogout() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("username", "admin")
                .param("password", "wrong"))
        .andExpect(status().isUnauthorized());
    var response =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("username", "admin")
                    .param("password", "test-password-123"))
            .andExpect(status().isNoContent())
            .andReturn();
    var session = (MockHttpSession) response.getRequest().getSession(false);
    assertNotNull(session);
    mvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"));
    mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void realCsrfTokenRequiredForLogin() throws Exception {
    var response = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    var token = mapper.readTree(response.getResponse().getContentAsString());
    mvc.perform(
            post("/api/auth/login")
                .session((MockHttpSession) response.getRequest().getSession(false))
                .header(token.get("headerName").asText(), token.get("token").asText())
                .param("username", "admin")
                .param("password", "test-password-123"))
        .andExpect(status().isNoContent());
  }
}
