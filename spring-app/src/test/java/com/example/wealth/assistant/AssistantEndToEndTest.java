package com.example.wealth.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wealth.FakeOllama;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AssistantEndToEndTest {

    static final FakeOllama ollama = FakeOllama.start();

    @DynamicPropertySource
    static void ollamaUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", ollama::url);
    }

    @AfterAll
    static void stopOllama() {
        ollama.stop();
    }

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void resetOllama() {
        ollama.reset();
    }

    @Test
    void answersWithToolResultForTheClientInTheHeader(CapturedOutput output) throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"",
                 "tool_calls":[{"function":{"name":"exposure","arguments":{"dimension":"SECTOR"}}}]},
                 "done":true,"done_reason":"stop"}""");
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"69.05% of your portfolio is in technology."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1002", "What's my exposure to tech?")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("69.05% of your portfolio is in technology."));

        assertThat(ollama.chatRequests()).hasSize(2);
        assertThat(ollama.chatRequests().get(1)).contains("TECHNOLOGY").contains("69.05");
        assertThat(output).contains("tool_call client=C-1002 tool=exposure").contains("outcome=success");
    }

    @Test
    void addsTheMatchingPolicySectionToTheQuestion() throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"Only GROWTH clients, up to 5%."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1001", "What does the investment policy say about crypto-assets?")
                .andExpect(status().isOk());

        assertThat(ollama.chatRequests()).singleElement().asString()
                .contains("Crypto-assets are permitted only for clients with the GROWTH risk profile.");
    }

    @Test
    void documentExcerptsDoNotStopTheModelFromUsingTools() throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"ok"},"done":true,"done_reason":"stop"}""");

        ask("C-1002", "Is my crypto exposure within policy?").andExpect(status().isOk());

        // Spring AI's default advisor template says to answer only from the excerpts, which suppressed tool calls.
        assertThat(ollama.chatRequests()).singleElement().asString()
                .doesNotContain("If the answer is not in the context")
                .contains("For figures about the client's portfolio, use the tools");
    }

    @Test
    void policyCheckToolReportsTheBreachComputedInJava(CapturedOutput output) throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"",
                 "tool_calls":[{"function":{"name":"policyCheck","arguments":{}}}]},
                 "done":true,"done_reason":"stop"}""");
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"Crypto exceeds the 5% limit by 7.70 points."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1002", "Is my crypto exposure within policy?").andExpect(status().isOk());

        assertThat(ollama.chatRequests().get(1)).contains("Crypto-assets").contains("ABOVE_MAX")
                .contains("7.70").contains("4850.00");
        assertThat(output).contains("tool_call client=C-1002 tool=policyCheck");
    }

    @Test
    void rejectsUnknownClientWithoutCallingTheModel() throws Exception {
        ask("C-9999", "What's my exposure to tech?").andExpect(status().isForbidden());

        assertThat(ollama.chatRequests()).isEmpty();
    }

    private ResultActions ask(String clientId, String question) throws Exception {
        return mockMvc.perform(post("/assistant")
                .header("X-Client-Id", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"" + question + "\"}"));
    }
}
