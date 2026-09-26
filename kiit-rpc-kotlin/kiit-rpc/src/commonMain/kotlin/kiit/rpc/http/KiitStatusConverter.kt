package kiit.rpc.http

import kiit.codes.Status
import kiit.requests.Contents
import kiit.rpc.RpcResponse
import kiit.rpc.StatusConverter

/**
 * Default [StatusConverter]. Checks the `x-server-status-rfc9457` header first, no body access
 * needed. If [parseStatusFromBody] is true, parses the body for kiit's own `CodeDetail`/
 * `Problem` shapes next. Falls back to the HTTP status code table ([Int.toStatus]) last.
 *
 * A `class`, not an `object`, since [parseStatusFromBody] is per-instance. [HttpRpc] builds its
 * default from `RpcSettings.parseStatusFromBody` when no [StatusConverter] is supplied.
 */
class KiitStatusConverter(private val parseStatusFromBody: Boolean = false) : StatusConverter {
    override fun convert(response: RpcResponse): Status {
        headerStatusOrNull(response.meta)?.let { return it }
        if (parseStatusFromBody) {
            Contents.toText(response.data)?.let { text -> structuredStatusOrNull(text)?.let { return it } }
        }
        return response.status.toStatus()
    }
}

/** Convenience for the common case. See [HttpRpc]'s `statusConverter` setting for the pluggable path. */
fun RpcResponse.resolveStatus(): Status = KiitStatusConverter().convert(this)
