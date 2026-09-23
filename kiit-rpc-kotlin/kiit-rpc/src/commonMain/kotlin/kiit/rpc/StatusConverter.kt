package kiit.rpc

import kiit.codes.Status

/**
 * Resolves the [Status] for an [HttpRpcResponse]. The default ([KiitStatusConverter]) recognizes
 * kiit's own structured error shapes, `CodeDetail`/`Problem`, first, falling back to the HTTP
 * status code table ([Int.toStatus]). That fallback is deliberately lossy: kiit-codes only maps
 * Status to HTTP, never the reverse.
 *
 * A caller talking to a non-kiit API with its own structured error shape (Stripe's error JSON,
 * GitHub's, etc.) can supply their own [StatusConverter] instead.
 */
interface StatusConverter {
    fun convert(response: HttpRpcResponse): Status
}

/** Default [StatusConverter]: kiit's own `CodeDetail`/`Problem` recognition, HTTP-code table fallback. */
object KiitStatusConverter : StatusConverter {
    override fun convert(response: HttpRpcResponse): Status {
        val looksJson =
            response.headers.entries.any { (key, value) ->
                key.equals("Content-Type", ignoreCase = true) && value.contains("json", ignoreCase = true)
            }
        val fromBody = if (looksJson && response.body.isNotBlank()) structuredStatusOrNull(response.body) else null
        return fromBody ?: response.status.toStatus()
    }
}

/** Convenience for the common case. See [HttpRpc]'s statusConverter setting for the pluggable path. */
fun HttpRpcResponse.resolveStatus(): Status = KiitStatusConverter.convert(this)
