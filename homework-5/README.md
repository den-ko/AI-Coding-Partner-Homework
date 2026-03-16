# MCP — Lorem Ipsum

**Author:** Denys Kobernik

## Overview

A minimal [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server built with FastMCP that exposes content from a local markdown file to AI clients such as Claude.

## What Was Built

- **MCP server** (`server.py`) using the `FastMCP` framework
- **`read` tool** — callable by Claude to retrieve content from `lorem-ipsum.md`, with an optional `word_count` parameter; returns all words if omitted
- **`lorem-ipsum://` resource** — a URI-based resource endpoint that exposes a specified number of words as readable context
- **`.mcp.json`** — project-scoped MCP configuration for Claude CLI, enabling automatic server startup when running `claude` from this directory
- **`HOWTORUN.md`** — step-by-step instructions for installing dependencies, configuring Claude CLI, and using the `read` tool

## Stack

- Python 3.10+
- [`mcp[cli]`](https://pypi.org/project/mcp/) — MCP server framework
- [`uv`](https://docs.astral.sh/uv/) — dependency and environment management
