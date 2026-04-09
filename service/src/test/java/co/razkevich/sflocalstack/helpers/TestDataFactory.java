package co.razkevich.sflocalstack.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestDataFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestDataFactory() {}

    public static String createAccountJson(String name) {
        return toJson(Map.of("Name", name, "Industry", "Technology", "BillingCity", "San Francisco"));
    }

    public static String createContactJson(String firstName, String lastName) {
        return toJson(Map.of("FirstName", firstName, "LastName", lastName, "Email", firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com"));
    }

    public static String createLeadJson(String company, String lastName) {
        return toJson(Map.of("Company", company, "LastName", lastName, "Status", "Open"));
    }

    public static String createAccountViaApi(MockMvc mvc) throws Exception {
        return createAccountViaApi(mvc, "Test Account " + System.nanoTime());
    }

    public static String createAccountViaApi(MockMvc mvc, String name) throws Exception {
        MvcResult result = mvc.perform(post("/services/data/v60.0/sobjects/Account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAccountJson(name)))
                .andExpect(status().isCreated())
                .andReturn();
        Map<String, Object> body = MAPPER.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("id");
    }

    public static void resetOrg(MockMvc mvc) throws Exception {
        mvc.perform(post("/reset"))
                .andExpect(status().isOk());
    }

    private static String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
