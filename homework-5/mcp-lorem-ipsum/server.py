from mcp.server.fastmcp import FastMCP
import os

# Create an MCP server
mcp = FastMCP("Lorem Ipsum Service")

LOREM_FILE = os.path.join(os.path.dirname(__file__), "lorem-ipsum.md")


def _get_words(word_count: int | None = None) -> str:
    """Read lorem-ipsum.md and return exactly word_count words, or all words if word_count is None."""
    with open(LOREM_FILE) as f:
        content = f.read()
    words = content.split()
    return " ".join(words[:word_count] if word_count is not None else words)


@mcp.resource("lorem-ipsum://{word_count}")
def lorem_resource(word_count: int = 30) -> str:
    """Read lorem-ipsum.md and return the specified number of words as a resource."""
    return _get_words(word_count)


@mcp.tool()
def read(word_count: int | None = None) -> str:
    """Read content from lorem-ipsum.md. Returns all words if word_count is not provided, otherwise returns the specified number of words."""
    return _get_words(word_count)


# Run the server
if __name__ == "__main__":
    mcp.run()
