package kiit.rpc

/** Per-call override of `RpcSettings`' timeouts. A non-null field wins for this one call only. */
data class RpcOptions(
    val requestTimeoutMillis: Long? = null,
    /** No-op on Darwin (iOS), same limitation as `RpcSettings.connectTimeoutMillis`. */
    val connectTimeoutMillis: Long? = null,
    val socketTimeoutMillis: Long? = null,
)
