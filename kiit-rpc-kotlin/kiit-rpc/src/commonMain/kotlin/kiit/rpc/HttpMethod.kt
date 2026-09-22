package kiit.rpc

/** HTTP verbs HttpRpc supports. GET never carries a body, see [HttpRpcRequest]. */
enum class HttpMethod {
    Get,
    Post,
    Put,
    Patch,
    Delete,
}
