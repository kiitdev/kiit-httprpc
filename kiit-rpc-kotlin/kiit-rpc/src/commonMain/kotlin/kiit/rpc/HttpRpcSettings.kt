package kiit.rpc

/**
 * Client-wide configuration, translated into Ktor's `HttpTimeout` plugin + `followRedirects` at
 * [HttpRpc] construction. Anything this doesn't cover is reachable via the `engine`
 * (`HttpClientEngine?`) escape hatch on [HttpRpc] instead of growing this type indefinitely.
 */
data class HttpRpcSettings(
    val requestTimeoutMillis: Long? = null,
    val connectTimeoutMillis: Long? = null,
    val socketTimeoutMillis: Long? = null,
    val followRedirects: Boolean = true,
    val defaultHeaders: Map<String, String> = emptyMap(),
)
