# 0005. In-memory vector store, filled at startup, one chunk per section

Status: accepted

## Context
RAG covers three short policy documents (15 numbered sections). A database is a non-goal. Both apps should retrieve the same chunks so their answers can be compared.

## Decision
Each app splits the documents itself into one chunk per numbered section, embeds them with Ollama at startup and keeps them in the framework's in-memory vector store. Every question retrieves the 3 most similar sections and adds them to the prompt.

## Consequences
- No extra infrastructure; the splitting rule is the same in both apps and easy to explain.
- Startup needs a running Ollama and includes embedding time, which affects the startup measurements.
- Section chunking only works for small, well-structured documents; real document sets need a general splitter, a persistent store and offline ingestion.
- Retrieval runs on every question, including pure portfolio questions, which adds unrelated text to the prompt.
