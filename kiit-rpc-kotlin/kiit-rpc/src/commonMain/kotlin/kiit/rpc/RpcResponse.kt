package kiit.rpc

import kiit.inputs.ListMap
import kiit.inputs.Meta
import kiit.inputs.MetaMap
import kiit.requests.Content

/**
 * kiit-owned response type, never leaks Ktor's own response type through the public API.
 *
 * [meta] keeps every value for a repeated header (multiple `Set-Cookie`), not just the first.
 * [data] is never forced through text decoding: a binary response comes back as `ContentFile`/
 * `ContentData` with its raw bytes intact, a text response as `ContentText`.
 */
data class RpcResponse(
    val status: Int,
    val data: Content,
    val meta: Meta = MetaMap(ListMap()),
)
