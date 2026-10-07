# Setting up Ollama

Both apps talk to a local [Ollama](https://ollama.com) server (ADR 0004). Tests do not need it; running the apps does.

Ollama is installed inside the repository, in `.ollama/` (git-ignored): the binary, its libraries and the downloaded models. Nothing is installed system-wide, and deleting `.ollama/` removes it completely. The version is pinned in `scripts/ollama.sh` (`OLLAMA_VERSION`, currently 0.35.1).

## Models

The default model is `qwen2.5:1.5b`: a small model with tool-calling support, about 1 GB on disk, chosen to fit in 2 GB of VRAM (or run on CPU). It is meant for trying the apps, not for judging answer quality: it calls tools less reliably and phrases answers worse than larger models. To use a larger one, set `OLLAMA_CHAT_MODEL` (e.g. `qwen3:8b`) both when pulling and when starting an app.

Retrieval over the policy documents uses a separate embedding model, `nomic-embed-text` (about 300 MB), set with `OLLAMA_EMBEDDING_MODEL`. `scripts/ollama.sh pull` downloads both. The apps embed the documents at startup, so Ollama must be running before an app starts.

## Linux and macOS

Linux needs `curl` and `zstd`; macOS needs `curl` and `unzip`.

```
scripts/ollama.sh install     # download Ollama into .ollama/
scripts/ollama.sh serve       # start the server on 127.0.0.1:11434; keep this terminal open
scripts/ollama.sh pull        # in a second terminal: download both models into .ollama/models
```

Check the server is up:

```
curl http://localhost:11434/api/tags
```

Then start an app as described in the README ("How to run").

## Windows

The script is bash only. In PowerShell, from the repository root:

```
New-Item -ItemType Directory -Force .ollama | Out-Null
Invoke-WebRequest "https://ollama.com/download/ollama-windows-amd64.zip?version=0.35.1" -OutFile .ollama\ollama.zip
Expand-Archive .ollama\ollama.zip -DestinationPath .ollama -Force
$env:OLLAMA_MODELS = "$PWD\.ollama\models"
.ollama\ollama.exe serve
```

In a second PowerShell window, set `OLLAMA_MODELS` the same way and run `.ollama\ollama.exe pull qwen2.5:1.5b` and `.ollama\ollama.exe pull nomic-embed-text`.

## Troubleshooting

- `address already in use` on `serve`: a system-wide Ollama is already running on port 11434. Stop it, or use it instead of the local one.
- `Connection refused` when an app starts or answers: the server is not running, or `OLLAMA_BASE_URL` is wrong.
- `model "..." not found`: the model was not pulled, or `OLLAMA_CHAT_MODEL` differs between `pull` and the app.
- The answer ignores the portfolio or invents figures: the model did not call the tools. This is common with small models; try a larger one.
- The first request is slow: Ollama loads the model into memory on first use.
