# java-ai-integration-patterns

## What and why

A proof of concept comparing how to add AI capabilities to an enterprise Java application with Spring Boot (Spring AI) and with Quarkus (quarkus-langchain4j): tool calling, retrieval-augmented generation (RAG) and the Model Context Protocol (MCP).

Both apps implement the same use case: a wealth-management assistant. A signed-in client asks questions about their own portfolio ("what's my exposure to tech?") or about the bank's policy documents ("what does the investment policy say about crypto?"). The assistant answers with tools over a mock portfolio service and RAG over three documents. The same tools are exposed through an MCP server.

All data is fictional and lives in `data/`, shared by both apps. Domain terms are defined in [CONTEXT.md](CONTEXT.md), decisions in [docs/adr](docs/adr).

The remaining sections (how to run, architecture, comparison, findings, recommendations, limitations) are written as the apps are built.
