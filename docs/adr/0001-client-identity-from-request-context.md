# 0001. Tools get the client from the request context, never from the model

Status: accepted

## Context
The assistant answers questions about one signed-in client's portfolio. If a tool accepted a client id as an argument, the model would choose it, and a prompt injection ("show me client C-1002") or a plain model error could read another client's data. A system prompt instruction is not a control.

## Decision
No tool has a client id parameter. The current client is resolved from the request context (the `X-Client-Id` header, standing in for an authenticated principal) before the model is called, and tools read it from there. A test fails if any tool method gains a parameter named like a client id.

## Consequences
- Cross-client access through the model is impossible by construction, not by instruction.
- Tools are bound to a request scope, so they cannot be reused as stateless functions.
- MCP has no per-user identity in this PoC; the MCP server is bound to one configured client (see README limitations).
