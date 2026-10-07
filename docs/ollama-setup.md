# Setting up Ollama

Both apps talk to a local [Ollama](https://ollama.com) server (ADR 0004). Tests do not need it; running the apps does.

## 1. Install

- macOS: download the app from https://ollama.com/download, or `brew install ollama`.
- Linux: `curl -fsSL https://ollama.com/install.sh | sh`
- Windows: download the installer from https://ollama.com/download.

Check the install with `ollama --version`.

## 2. Start the server

The desktop app starts the server on its own. Otherwise run:

```
ollama serve
```

It listens on `http://localhost:11434`. Check it is up:

```
curl http://localhost:11434/api/tags
```

## 3. Pull the model

```
ollama pull qwen3:8b
```

The download is several gigabytes. The assistant needs a model that supports tool calling; `ollama show qwen3:8b` lists `tools` under its capabilities. On a machine with little memory, a smaller model such as `qwen3:4b` also works, with less reliable tool use.

## 4. Point the apps at it

The defaults match a standard local install. To use another model or server, set:

```
export OLLAMA_CHAT_MODEL=qwen3:4b
export OLLAMA_BASE_URL=http://localhost:11434
```

Then start an app as described in the README ("How to run").

## Troubleshooting

- `Connection refused` on startup or first request: the server is not running, or `OLLAMA_BASE_URL` is wrong.
- `model "..." not found`: the model was not pulled, or `OLLAMA_CHAT_MODEL` does not match a name in `ollama list`.
- The answer ignores the portfolio or invents figures: the model did not call the tools. Try a larger model; tool calling quality varies between models.
- The first request is slow: Ollama loads the model into memory on first use.
