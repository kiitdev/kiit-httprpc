package kiit.rpc.http

import kiit.codes.Excluded
import kiit.codes.Invalid
import kiit.codes.Pending
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.Status
import kiit.codes.Succeeded
import kiit.codes.Unserved

/**
 * Fallback used when a response carries no `CodeDetail` body (any non-kiit API). kiit-codes only
 * maps Status to HTTP ([kiit.codes.CodesToHttp]), never the reverse, since many statuses share
 * one HTTP code. This table is kiit-rpc's own: one canonical [Status] per code, picked for the
 * closest real-world HTTP semantic rather than a mechanical inversion of CodesToHttp's overrides.
 */
private val specificHttpStatusCodes: Map<Int, Status> =
    mapOf(
        200 to Succeeded.SUCCESS,
        201 to Succeeded.CREATED,
        202 to Pending.ACCEPTED,
        204 to Succeeded.HANDLED,
        307 to Pending.REDIRECTED,
        400 to Invalid.BAD_REQUEST,
        401 to Restricted.UNAUTHENTICATED,
        403 to Restricted.FORBIDDEN,
        404 to Invalid.NOT_FOUND,
        409 to Rejected.CONFLICT,
        410 to Rejected.GONE,
        413 to Invalid.PAYLOAD_TOO_LARGE,
        423 to Restricted.LOCKED,
        429 to Unserved.RATE_LIMITED,
        451 to Unserved.LEGAL_BLOCK,
        499 to Excluded.CANCELLED,
        500 to Unserved.UNEXPECTED,
        501 to Unserved.UNSUPPORTED,
        503 to Unserved.UNDER_MAINTENANCE,
        504 to Unserved.TIMEOUT,
    )

/** Resolves a raw HTTP status code to a [Status]. See [specificHttpStatusCodes]. */
fun Int.toStatus(): Status {
    specificHttpStatusCodes[this]?.let { return it }
    return when (this) {
        in 200..299 -> Succeeded.SUCCESS
        in 300..399 -> Pending.REDIRECTED
        in 400..499 -> Invalid.INVALID_VALUE
        else -> Unserved.UNEXPECTED
    }
}
