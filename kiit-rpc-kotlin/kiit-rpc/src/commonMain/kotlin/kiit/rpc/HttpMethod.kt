package kiit.rpc

/** HTTP verbs supported by [kiit.httprpc.HttpRpc]. GET never carries a body — see [HttpRpcRequest]. */
enum class HttpMethod {
    Get,
    Post,
    Put,
    Patch,
    Delete,
}
