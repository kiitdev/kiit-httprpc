package kiit.rpc

/**
 * HTTP verbs HttpRpc supports. GET never carries a body, see [HttpRpcRequest]. QUERY is the
 * newer safe method that allows one: read-only like GET, but for queries too complex or large
 * for a URL.
 */
enum class HttpMethod {
    Get,
    Query,
    Post,
    Put,
    Patch,
    Delete,
}
