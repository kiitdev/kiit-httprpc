package kiit.rpc

/**
 * The final, fully-resolved request — built once per call, after query-param/auth/body
 * resolution but before the [Policy] chain and the actual Ktor call. This is what a logging or
 * diagnostics [Policy] actually sees, and it's engine-agnostic (no Ktor types leak into it).
 *
 * [headers] includes the resolved `Authorization` header from [Auth] — a [Policy] that logs
 * headers verbatim will leak bearer tokens/basic-auth credentials. That's the caller's
 * responsibility to redact, not something guarded here.
 */
data class HttpRpcRequest(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)
