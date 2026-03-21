package co.razkevich.sflocalstack.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExternalIdUpsertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void concurrentUpsertsDoNotCreateDuplicates() throws Exception {
        mockMvc.perform(post("/reset")).andExpect(status().isOk());

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            final int value = index;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(patch("/services/data/v60.0/sobjects/Account/External_Id__c/EXT-CONCURRENT")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"Name\":\"Concurrent " + value + "\"}"))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor));
        }

        List<Integer> statuses = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdownNow();

        assertThat(statuses).allMatch(status -> status == 201 || status == 200);

        String response = mockMvc.perform(get("/services/data/v60.0/query")
                        .param("q", "SELECT Id, Name FROM Account WHERE External_Id__c = 'EXT-CONCURRENT'"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"totalSize\":1");
    }
}
