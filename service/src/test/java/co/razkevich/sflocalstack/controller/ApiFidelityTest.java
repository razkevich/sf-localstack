package co.razkevich.sflocalstack.controller;

import co.razkevich.sflocalstack.model.SalesforceIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for Salesforce API fidelity: error format, headers, ID format,
 * describe shape, and query response shape.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiFidelityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());
    }

    // --- Error response format ---

    @Test
    void errorResponseIncludesFieldsArray() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "NOT VALID SOQL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].errorCode").value("MALFORMED_QUERY"))
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].fields").isArray())
                .andExpect(jsonPath("$[0].fields").isEmpty());
    }

    @Test
    void notFoundErrorResponseIncludesFieldsArray() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/001000000000000AAA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$[0].errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].fields").isArray());
    }

    // --- Sforce-Limit-Info header ---

    @Test
    void sforceLimitInfoHeaderPresentOnRestQuery() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Header Test\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id FROM Account"))
                .andExpect(status().isOk())
                .andExpect(header().string("Sforce-Limit-Info", "api-usage=0/15000"));
    }

    @Test
    void sforceLimitInfoHeaderPresentOnSobjectCreate() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Header Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Sforce-Limit-Info", "api-usage=0/15000"));
    }

    @Test
    void sforceLimitInfoHeaderPresentOnVersionsEndpoint() throws Exception {
        mockMvc.perform(get("/services/data/"))
                .andExpect(status().isOk())
                .andExpect(header().string("Sforce-Limit-Info", "api-usage=0/15000"));
    }

    @Test
    void sforceLimitInfoHeaderAbsentOnNonServicesEndpoint() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Sforce-Limit-Info"));
    }

    // --- ID format ---

    @Test
    void generatedIdIsValid18CharSalesforceId() throws Exception {
        String response = mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"ID Test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();
        assertThat(id).hasSize(18);
        assertThat(id).startsWith("001");
        assertThat(SalesforceIdGenerator.isValidFormat(id)).isTrue();

        // Verify suffix is correct
        String base15 = id.substring(0, 15);
        String expectedSuffix = SalesforceIdGenerator.computeSuffix(base15);
        assertThat(id.substring(15)).isEqualTo(expectedSuffix);
    }

    @Test
    void contactIdStartsWith003() throws Exception {
        String response = mockMvc.perform(post("/services/data/v60.0/sobjects/Contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"LastName\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();
        assertThat(id).hasSize(18);
        assertThat(id).startsWith("003");
        assertThat(SalesforceIdGenerator.isValidFormat(id)).isTrue();
    }

    // --- Describe response shape ---

    @Test
    void describeIncludesRecordTypeInfos() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordTypeInfos").isArray())
                .andExpect(jsonPath("$.recordTypeInfos[0].active").value(true))
                .andExpect(jsonPath("$.recordTypeInfos[0].available").value(true))
                .andExpect(jsonPath("$.recordTypeInfos[0].defaultRecordTypeMapping").value(true))
                .andExpect(jsonPath("$.recordTypeInfos[0].developerName").value("Master"))
                .andExpect(jsonPath("$.recordTypeInfos[0].master").value(true))
                .andExpect(jsonPath("$.recordTypeInfos[0].name").value("Master"))
                .andExpect(jsonPath("$.recordTypeInfos[0].recordTypeId").value("012000000000000AAA"));
    }

    @Test
    void describeIncludesChildRelationshipsForAccount() throws Exception {
        mockMvc.perform(get("/services/data/v60.0/sobjects/Account/describe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childRelationships").isArray())
                .andExpect(jsonPath("$.childRelationships[?(@.relationshipName == 'Contacts')].childSObject").value("Contact"))
                .andExpect(jsonPath("$.childRelationships[?(@.relationshipName == 'Opportunities')].childSObject").value("Opportunity"));
    }

    // --- Query response shape ---

    @Test
    void queryResponseIncludesAttributesWithTypeAndUrl() throws Exception {
        mockMvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"Name\":\"Query Shape Test\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSize").isNumber())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.records[0].attributes.type").value("Account"))
                .andExpect(jsonPath("$.records[0].attributes.url").exists())
                .andExpect(jsonPath("$.records[0].Id").exists())
                .andExpect(jsonPath("$.records[0].Name").exists());
    }

    // --- SalesforceIdGenerator unit-level checks ---

    @Test
    void salesforceIdGeneratorProducesConsistentSuffix() {
        // Known example: 001A000000012345 should compute a deterministic suffix
        String testId = "001Ax00000Abcde";
        String suffix = SalesforceIdGenerator.computeSuffix(testId);
        assertThat(suffix).hasSize(3);
        // Verify re-computation is stable
        assertThat(SalesforceIdGenerator.computeSuffix(testId)).isEqualTo(suffix);
    }

    @Test
    void salesforceIdTo15And18AreInverses() {
        String id18 = SalesforceIdGenerator.generate("001");
        assertThat(id18).hasSize(18);

        String id15 = SalesforceIdGenerator.to15(id18);
        assertThat(id15).hasSize(15);

        String id18Again = SalesforceIdGenerator.to18(id15);
        assertThat(id18Again).isEqualTo(id18);
    }

    @Test
    void salesforceIdEqualityAcceptsBoth15And18() {
        String id18 = SalesforceIdGenerator.generate("003");
        String id15 = SalesforceIdGenerator.to15(id18);
        assertThat(SalesforceIdGenerator.idsEqual(id18, id15)).isTrue();
        assertThat(SalesforceIdGenerator.idsEqual(id15, id18)).isTrue();
    }

    @Test
    void salesforceIdValidFormatAccepts15And18() {
        assertThat(SalesforceIdGenerator.isValidFormat("001A00000012345")).isTrue();
        assertThat(SalesforceIdGenerator.isValidFormat("001A00000012345AAA")).isTrue();
        assertThat(SalesforceIdGenerator.isValidFormat("short")).isFalse();
        assertThat(SalesforceIdGenerator.isValidFormat(null)).isFalse();
    }
}
