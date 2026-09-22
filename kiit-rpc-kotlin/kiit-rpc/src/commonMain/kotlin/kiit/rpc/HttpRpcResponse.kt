package kiit.rpc

/**
 * kiit-owned response type — never leaks Ktor's own response type through the public API.
 * The `kiit.codes.Status` for a given response is resolved separately (see status resolution)
 * and carried on the `Outcome<HttpRpcResponse>` wrapper, not stored here.
 */
data class HttpRpcResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
)
