package src.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// End-to-end check that the web layer (controllers, security config, DTOs, exception mapping)
// correctly drives the same LoaningSystem the CLI and plain unit tests already exercise —
// business-rule correctness is covered there; this confirms the HTTP plumbing sitting on top
// of it (auth, CSRF, role gates, request/response mapping) actually works together.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoaningSystemRestFlowTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static Path dbFile;

    // One Spring context (and one temp DB) shared across all tests in this class — each test
    // below creates its own uniquely-named applicant/staff so they don't depend on each other's
    // data or on any particular execution order.
    @DynamicPropertySource
    static void registerDbFile(DynamicPropertyRegistry registry) throws IOException {
        dbFile = Files.createTempFile("lms-rest-flow-test", ".db");
        Files.deleteIfExists(dbFile);
        registry.add("lms.db-file", dbFile::toString);
    }

    @AfterAll
    void tearDown() throws IOException {
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(Path.of(dbFile + "-wal"));
        Files.deleteIfExists(Path.of(dbFile + "-shm"));
    }

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
        return session;
    }

    @Test
    void fullLendingLifecycleThroughRestApi() throws Exception {
        MockHttpSession adminSession = loginAs("Admin123", "1234");

        MvcResult applicantResult = mockMvc.perform(post("/api/applicants")
                        .with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Jane Doe","username":"jane-rest","phoneNumber":"011111111",
                                 "password":"pass1234","age":30,"income":10000,"gender":"F","existingExternalDebt":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        int applicantId = objectMapper.readTree(applicantResult.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(post("/api/staff")
                        .with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Larry Officer","username":"larry-rest","phoneNumber":"022222222",
                                 "age":28,"password":"passL","salary":3000,"position":"LOANOFFICER"}
                                """))
                .andExpect(status().isCreated());

        MockHttpSession janeSession = loginAs("jane-rest", "pass1234");

        MvcResult createResult = mockMvc.perform(post("/api/contracts")
                        .with(csrf()).session(janeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applicantId\":" + applicantId + ",\"amount\":1200,\"duration\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        int contractId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asInt();

        MockHttpSession larrySession = loginAs("larry-rest", "passL");
        mockMvc.perform(post("/api/contracts/" + contractId + "/approve")
                        .with(csrf()).session(larrySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/applicants/" + applicantId + "/balance")
                        .with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountBalance").value(2000.00));

        MvcResult payResult = mockMvc.perform(post("/api/contracts/" + contractId + "/payments")
                        .with(csrf()).session(janeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1300}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode payment = objectMapper.readTree(payResult.getResponse().getContentAsString());
        assertEquals(1232.73, payment.get("amountApplied").asDouble(), 0.001);
        assertEquals(767.27, payment.get("remainingBalance").asDouble(), 0.001);

        mockMvc.perform(get("/api/contracts/" + contractId + "/schedule").session(janeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullyPaid").value(true));
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/contracts/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"Admin123\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void applicantCannotAccessManagerOnlyAuditLog() throws Exception {
        MockHttpSession adminSession = loginAs("Admin123", "1234");
        mockMvc.perform(post("/api/applicants")
                        .with(csrf()).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bob Roe","username":"bob-rest","phoneNumber":"099999999",
                                 "password":"pass1234","age":40,"income":5000,"gender":"M","existingExternalDebt":0}
                                """))
                .andExpect(status().isCreated());

        MockHttpSession bobSession = loginAs("bob-rest", "pass1234");
        mockMvc.perform(get("/api/audit-log").session(bobSession))
                .andExpect(status().isForbidden());
    }
}
