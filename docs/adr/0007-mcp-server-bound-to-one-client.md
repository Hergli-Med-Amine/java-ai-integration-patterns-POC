# 0007. The MCP server acts for one configured client

Status: accepted

## Context
The same portfolio tools are exposed over MCP so other AI clients (e.g. an IDE assistant or a desktop chat app) can use them. ADR 0001 requires the client to come from the request context, never from the model. In this PoC an MCP request carries no user identity: there is no authentication, and the MCP client's own model is outside our control.

## Decision
The MCP server is bound to one client, set by `WEALTH_MCP_CLIENT_ID` (`wealth.mcp.client-id`). There is no default: without it, no MCP tools are registered. Every MCP tool call gets the configured client in its tool context and is audited like the assistant's tool calls. The app refuses to start if the configured client does not exist. The app listens on 127.0.0.1 only, and the MCP resource and prompt features are switched off. The server uses the stateless MCP protocol: tools only, no session kept between requests.

## Consequences
- The guardrail holds over MCP too: no tool takes a client id, and the MCP SDK rejects arguments the tool schema does not declare.
- Once a client is configured, anyone who can reach `/mcp` reads that client's data. This is the main gap of the PoC; binding to 127.0.0.1 limits it to the local machine.
- The tools are not marked read-only for MCP clients: Spring AI 2.0.1 sets these hints only through its separate `@McpTool` annotation. The MCP specification treats the hints as untrusted anyway.
- Per-user MCP needs authentication on the MCP endpoint (the MCP specification uses OAuth 2.1) and mapping the authenticated user to a client id, in place of the configured one.
- Stateless mode rules out server-initiated notifications, which these tools do not need.
