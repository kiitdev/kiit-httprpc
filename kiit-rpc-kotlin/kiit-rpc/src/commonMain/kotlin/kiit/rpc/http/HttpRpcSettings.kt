package kiit.rpc.http

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
    /**
     * Prefixed onto every call's `url` that isn't already absolute (doesn't start with `http://`
     * or `https://`), so a client talking to one API can pass `"/users"` instead of the full
     * `"https://api.example.com/users"` on every call. A call that already passes an absolute
     * URL is sent as-is, `baseUrl` or not, e.g. for one-off calls to a different host.
     */
    val baseUrl: String? = null,
)
