# How to Run the Lorem Ipsum MCP Server

## Prerequisites

- Python 3.10+
- [uv](https://docs.astral.sh/uv/) package manager
- [Claude Code CLI](https://docs.anthropic.com/en/docs/claude-code)

---

## 1. Install Dependencies

```bash
uv sync
```

---

## 2. Register the MCP Server with Claude CLI

The `.mcp.json` file in this project root is already configured. Claude CLI automatically reads it when launched from this directory.

To start Claude from the project directory:

```bash
cd /Users/denk/VSCode/mcp_demo
claude
```

---

## 3. Start Claude

```bash
claude
```

Claude will spawn the MCP server in the background. The `read` tool will be available immediately.

---

## 4. Use the `read` Tool

### Return all words
> "Use the read tool to read the lorem ipsum file."

Claude calls `read()` and returns the full content of `lorem-ipsum.md`.

### Return a specific number of words
> "Use the read tool to get the first 10 words from the lorem ipsum file."

Claude calls `read(word_count=10)` and returns exactly 10 words.
