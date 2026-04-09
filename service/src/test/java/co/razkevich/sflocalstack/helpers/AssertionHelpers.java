package co.razkevich.sflocalstack.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public final class AssertionHelpers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AssertionHelpers() {}

    /**
     * Asserts a 201 Created response with Salesforce create shape: {id, success: true, errors: []}
     */
    public static void assertCreatedResponse(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Map<String, Object> body = parseJson(result);
        assertThat(body).containsKey("id");
        assertThat(body.get("id")).isNotNull();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat((List<?>) body.get("errors")).isEmpty();
    }

    /**
     * Asserts a Salesforce-style error response: [{message: "...", errorCode: "...", fields: [...]}]
     */
    public static void assertSalesforceError(MvcResult result, int expectedStatus, String expectedErrorCode) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        String content = result.getResponse().getContentAsString();
        // SF errors can be array or object
        if (content.startsWith("[")) {
            List<Map<String, Object>> errors = MAPPER.readValue(content, List.class);
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0)).containsKey("errorCode");
            if (expectedErrorCode != null) {
                assertThat(errors.get(0).get("errorCode")).isEqualTo(expectedErrorCode);
            }
        } else {
            Map<String, Object> error = MAPPER.readValue(content, Map.class);
            assertThat(error).containsKey("errorCode");
        }
    }

    /**
     * Asserts a Salesforce query result shape: {totalSize: N, done: true, records: [...]}
     */
    public static void assertQueryResult(MvcResult result, int expectedSize) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> body = parseJson(result);
        assertThat(body).containsKeys("totalSize", "done", "records");
        assertThat(body.get("done")).isEqualTo(true);
        assertThat(((Number) body.get("totalSize")).intValue()).isEqualTo(expectedSize);
        assertThat((List<?>) body.get("records")).hasSize(expectedSize);
    }

    /**
     * Asserts a record has the Salesforce attributes wrapper: {attributes: {type: "...", url: "..."}, ...}
     */
    public static void assertRecordShape(String json, String expectedObjectType) throws Exception {
        Map<String, Object> record = MAPPER.readValue(json, Map.class);
        assertThat(record).containsKey("attributes");
        Map<String, Object> attrs = (Map<String, Object>) record.get("attributes");
        assertThat(attrs).containsKey("type");
        assertThat(attrs.get("type")).isEqualTo(expectedObjectType);
        assertThat(attrs).containsKey("url");
    }

    /**
     * Extracts the "id" field from a JSON response body.
     */
    public static String extractId(MvcResult result) throws Exception {
        Map<String, Object> body = parseJson(result);
        return (String) body.get("id");
    }

    private static Map<String, Object> parseJson(MvcResult result) throws Exception {
        return MAPPER.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
