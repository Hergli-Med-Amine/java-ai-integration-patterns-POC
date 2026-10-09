package com.example.wealth.assistant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.example.wealth.AuditLog;
import com.example.wealth.FakeOllama;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithTestResource(FakeOllama.Resource.class)
class AssistantEndToEndTest {

    static final FakeOllama ollama = FakeOllama.INSTANCE;

    @BeforeEach
    void reset() {
        ollama.reset();
        AuditLog.clear();
    }

    @Test
    void answersWithToolResultForTheClientInTheHeader() throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"",
                 "tool_calls":[{"function":{"name":"exposure","arguments":{"dimension":"SECTOR"}}}]},
                 "done":true,"done_reason":"stop"}""");
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"69.05% of your portfolio is in technology."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1002", "What's my exposure to tech?")
                .statusCode(200)
                .body("answer", equalTo("69.05% of your portfolio is in technology."));

        assertThat(ollama.chatRequests()).hasSize(2);
        assertThat(ollama.chatRequests().get(1)).contains("TECHNOLOGY").contains("69.05");
        assertThat(AuditLog.lines()).anySatisfy(line -> assertThat(line)
                .startsWith("tool_call client=C-1002 tool=exposure").contains("outcome=success"));
    }

    @Test
    void noToolLetsTheModelChooseTheClient() throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"Hello."},"done":true,"done_reason":"stop"}""");

        ask("C-1001", "Hello").statusCode(200);

        JsonNode tools = new ObjectMapper().readTree(ollama.chatRequests().getFirst()).get("tools");
        assertThat(tools).extracting(tool -> tool.at("/function/name").asText())
                .containsExactlyInAnyOrder("portfolioSummary", "holdings", "exposure", "policyCheck");
        assertThat(tools).allSatisfy(tool -> assertThat(tool.at("/function/parameters").toString())
                .as("parameters the model sees for tool '%s'", tool.at("/function/name").asText())
                .doesNotContainIgnoringCase("client"));
    }

    @Test
    void addsTheMatchingPolicySectionToTheQuestion() throws Exception {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"Only GROWTH clients, up to 5%."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1001", "What does the investment policy say about crypto-assets?").statusCode(200);

        assertThat(ollama.chatRequests()).singleElement().asString()
                .contains("Crypto-assets are permitted only for clients with the GROWTH risk profile.");
        assertThat(new ObjectMapper().readTree(ollama.chatRequests().getFirst()).get("messages"))
                .as("only the system prompt and this question: nothing remembered from earlier requests")
                .hasSize(2);
    }

    @Test
    void documentExcerptsDoNotStopTheModelFromUsingTools() {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"Let me check."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1002", "Is my crypto exposure within policy?").statusCode(200);

        assertThat(ollama.chatRequests()).singleElement().asString()
                .contains("For figures about the client's portfolio, use the tools");
    }

    @Test
    void policyCheckIsComputedInCodeAndReturnedToTheModel() {
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"",
                 "tool_calls":[{"function":{"name":"policyCheck","arguments":{}}}]},
                 "done":true,"done_reason":"stop"}""");
        ollama.respond("""
                {"model":"test","message":{"role":"assistant","content":"Your crypto exposure is above the 5% limit."},
                 "done":true,"done_reason":"stop"}""");

        ask("C-1002", "Is my crypto exposure within policy?").statusCode(200);

        assertThat(ollama.chatRequests()).hasSize(2);
        assertThat(ollama.chatRequests().get(1))
                .contains("Crypto-assets").contains("ABOVE_MAX").contains("7.70").contains("4850.00");
        assertThat(AuditLog.lines()).anySatisfy(line -> assertThat(line)
                .startsWith("tool_call client=C-1002 tool=policyCheck").contains("outcome=success"));
    }

    @Test
    void rejectsUnknownClientWithoutCallingTheModel() {
        ask("C-9999", "What's my exposure to tech?").statusCode(403);

        assertThat(ollama.chatRequests()).isEmpty();
    }

    @Test
    void publishesTheOpenApiDescriptionAndSwaggerUi() {
        given().get("/q/openapi?format=json").then().statusCode(200).body("paths.'/assistant'.post", notNullValue());
        given().get("/q/swagger-ui/").then().statusCode(200);
    }

    private static ValidatableResponse ask(String clientId, String question) {
        return given()
                .header("X-Client-Id", clientId)
                .contentType(ContentType.JSON)
                .body("{\"question\":\"" + question + "\"}")
                .post("/assistant")
                .then();
    }
}
