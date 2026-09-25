package kiit.rpc

import kiit.codes.Status

/**
 * Resolves the [Status] for an [RpcResponse]. The default ([kiit.rpc.http.KiitStatusConverter])
 * checks the `x-server-status-rfc9457` header first, then optionally the body, then falls back
 * to the raw HTTP status code (lossy: multiple kiit statuses can share one HTTP code).
 *
 * A caller talking to a non-kiit API with its own error shape (Stripe's, GitHub's) can supply
 * their own [StatusConverter] instead.
 */
interface StatusConverter {
    fun convert(response: RpcResponse): Status
}
