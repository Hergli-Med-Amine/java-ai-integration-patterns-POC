#!/usr/bin/env bash
# Installs and runs Ollama inside this repository (.ollama/) instead of system-wide.
set -euo pipefail

OLLAMA_VERSION="${OLLAMA_VERSION:-0.35.1}"
CHAT_MODEL="${OLLAMA_CHAT_MODEL:-qwen2.5:1.5b}"
EMBEDDING_MODEL="${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}"
INSTALL_DIR="$(cd "$(dirname "$0")/.." && pwd)/.ollama"
export OLLAMA_MODELS="$INSTALL_DIR/models"
export OLLAMA_HOST="127.0.0.1:11434"

binary() {
    if [ "$(uname -s)" = Darwin ]; then
        echo "$INSTALL_DIR/Ollama.app/Contents/Resources/ollama"
    else
        echo "$INSTALL_DIR/bin/ollama"
    fi
}

install() {
    mkdir -p "$INSTALL_DIR" "$OLLAMA_MODELS"
    local url="https://ollama.com/download"
    case "$(uname -s)-$(uname -m)" in
        Linux-x86_64)
            curl -fL --progress-bar "$url/ollama-linux-amd64.tar.zst?version=$OLLAMA_VERSION" | zstd -d | tar x -C "$INSTALL_DIR" ;;
        Linux-aarch64 | Linux-arm64)
            curl -fL --progress-bar "$url/ollama-linux-arm64.tar.zst?version=$OLLAMA_VERSION" | zstd -d | tar x -C "$INSTALL_DIR" ;;
        Darwin-*)
            curl -fL --progress-bar -o "$INSTALL_DIR/Ollama-darwin.zip" "$url/Ollama-darwin.zip?version=$OLLAMA_VERSION"
            unzip -q -o "$INSTALL_DIR/Ollama-darwin.zip" -d "$INSTALL_DIR"
            rm "$INSTALL_DIR/Ollama-darwin.zip" ;;
        *)
            echo "Unsupported platform $(uname -s)-$(uname -m); see docs/ollama-setup.md" >&2
            exit 1 ;;
    esac
    echo "Installed Ollama $OLLAMA_VERSION to $INSTALL_DIR"
}

case "${1:-}" in
    install) install ;;
    serve) exec "$(binary)" serve ;;
    pull) "$(binary)" pull "$CHAT_MODEL" && "$(binary)" pull "$EMBEDDING_MODEL" ;;
    *) echo "usage: scripts/ollama.sh install | serve | pull" >&2; exit 1 ;;
esac
