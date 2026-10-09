# java-ai-integration-patterns

## What and why

A proof of concept comparing how to add AI capabilities to an enterprise Java application with Spring Boot (Spring AI) and with Quarkus (quarkus-langchain4j): tool calling, retrieval-augmented generation (RAG) and the Model Context Protocol (MCP).

Both apps implement the same use case: a wealth-management assistant. A signed-in client asks questions about their own portfolio ("what's my exposure to tech?") or about the bank's policy documents ("what does the investment policy say about crypto?"). The assistant answers with tools over a mock portfolio service and RAG over three documents. The same tools are exposed through an MCP server.

All data is fictional and lives in `data/`, shared by both apps. Domain terms are defined in [CONTEXT.md](CONTEXT.md), decisions in [docs/adr](docs/adr).

## How to run

Requirements: Java 21, Maven 3.9, and on Linux/macOS `curl` plus `zstd` or `unzip`. Ollama is installed inside the repository by a script; setup, Windows steps and troubleshooting are in [docs/ollama-setup.md](docs/ollama-setup.md).

```
scripts/ollama.sh install
scripts/ollama.sh serve        # keep running; continue in a second terminal
scripts/ollama.sh pull
cd spring-app
mvn spring-boot:run
curl -s localhost:8080/assistant -H 'X-Client-Id: C-1002' -H 'Content-Type: application/json' \
     -d '{"question":"What is my exposure to tech?"}'
```

Instead of curl, open Swagger UI at http://localhost:8080/swagger-ui/index.html, choose "Try it out" on `POST /assistant`, and set the client id and question. It is there to make the PoC easier to try locally, not part of the architecture being compared.

The same tools can be served over MCP at `http://localhost:8080/mcp` (streamable HTTP, stateless). MCP requests carry no user identity in this PoC, so the MCP server acts for one client that you choose explicitly, and exposes no tools otherwise (ADR 0007):

```
WEALTH_MCP_CLIENT_ID=C-1001 mvn spring-boot:run
npx @modelcontextprotocol/inspector     # transport "Streamable HTTP", URL http://localhost:8080/mcp, then Tools > List Tools
```

The app listens on 127.0.0.1 only.

The Quarkus app does the same on port 8081, with the same data, environment variables and behaviour:

```
cd quarkus-app
mvn quarkus:dev                                   # or: WEALTH_MCP_CLIENT_ID=C-1001 mvn quarkus:dev
curl -s localhost:8081/assistant -H 'X-Client-Id: C-1002' -H 'Content-Type: application/json' \
     -d '{"question":"What is my exposure to tech?"}'
```

Swagger UI is at http://localhost:8081/q/swagger-ui and MCP at `http://localhost:8081/mcp`. Quarkus Dev Services are switched off, so no Docker is needed.

`OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL` and `OLLAMA_EMBEDDING_MODEL` override the defaults. `X-Client-Id` stands in for an authenticated user (ADR 0001); the fixture clients are `C-1001`, `C-1002` and `C-1003`. Tests run without Ollama: `mvn test`.

The remaining sections (architecture, comparison, findings, recommendations, limitations) are written as the apps are built.

## Disclosure

This project is a proof of concept, built to compare integration patterns and to support the findings above. It is not a finished product and is not meant for production use: authentication, persistence and operations are deliberately out of scope, and all data is fictional.

It was co-developed with Claude Code, using AI-assisted development.
