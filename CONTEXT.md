# Domain glossary

All data is fictional. All amounts are in EUR; there is no currency conversion.

**Client** – a person with a portfolio at the bank, identified by a client id (e.g. `C-1001`). Has exactly one risk profile.

**Current client** – the client on whose behalf a request is made. Taken from the request context (the `X-Client-Id` header in this PoC), never from the model. See ADR 0001.

**Risk profile** – `CONSERVATIVE`, `BALANCED` or `GROWTH`. Determines which limits of the investment policy apply.

**Holding** – a position in one instrument: symbol, name, asset class, sector, region, quantity and price.

**Asset class** – `EQUITY`, `BOND`, `FUND`, `CRYPTO` or `CASH`.

**Market value** – quantity × price of a holding.

**Portfolio value** – sum of the market values of all holdings of a client.

**Exposure** – share of portfolio value held in one sector, region or asset class, as a percentage. Funds are not looked through: a broad index fund counts as sector `DIVERSIFIED`, region `GLOBAL`.

**MCP client id** – the one client the MCP server acts for, set in configuration. MCP requests carry no user identity in this PoC, so they cannot choose a client. See ADR 0007.

**Policy check** – comparison of a client's portfolio with the investment policy limits that the portfolio data can answer: the asset-class ranges of the client's risk profile and the crypto-asset rules. Done in code by the `policyCheck` tool, never by the model. See ADR 0006.

**Policy document** – a plain-text document in `data/documents/` that applies to all clients (investment policy, fee schedule, risk disclosure). The assistant answers questions about these via retrieval (RAG), not via tools.

**Document section** – one numbered section of a policy document, the unit that is embedded and retrieved. Each section carries its document's title.

**Tool** – an operation the model may call, exposing a portfolio query for the current client. Tools have no client id parameter.

**Tool call** – one invocation of a tool by the model or by an MCP client.

**Audit record** – the record written for every tool call: client id, tool name, arguments, outcome (success or error) and timestamp.
