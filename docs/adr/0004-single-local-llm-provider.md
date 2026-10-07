# 0004. One LLM provider: Ollama, running locally

Status: accepted

## Context
Both frameworks abstract several providers. Supporting more than one would add configuration and test paths without adding to the comparison. Client portfolio data should not leave the machine in a PoC that may be run on a laptop.

## Decision
Both apps use Ollama only. Base URL and model come from `OLLAMA_BASE_URL` and `OLLAMA_CHAT_MODEL` (defaults `http://localhost:11434` and `qwen3:8b`). Tests replace Ollama with a fake HTTP server, so they need no model and no network.

## Consequences
- No API keys, no cost, no data sent to a third party.
- Answer quality and tool-calling reliability depend on the local model and are lower than hosted frontier models; findings about answer quality do not transfer to other providers.
- Switching provider means changing one dependency and a few properties per app.
