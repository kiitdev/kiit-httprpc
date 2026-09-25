@file:OptIn(ExperimentalUuidApi::class)

package kiit.rpc

import kiit.call.Trace
import kiit.call.Verb
import kiit.call.Version
import kiit.inputs.Inputs
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The caller-facing, outbound mirror of kiit-requests' `Request`. Shares its vocabulary
 * (`Verb`/`Version`/`Trace`), but uses a flat `url` instead of the `path`/`parts` convention,
 * since an outbound call isn't necessarily hitting a kiit API.
 *
 * No `callerId` field. Identity belongs to the calling service, set once at startup, not
 * something that changes per call the way `auth`/`options` do. It lives on `RpcSettings`.
 */
data class RpcRequest(
    val verb: Verb,
    val url: String,
    val meta: Inputs? = null,
    val args: Inputs? = null,
    val data: Body? = null,
    val auth: Auth? = null,
    val version: Version? = null,
    val trace: Trace? = null,
    val requestId: String = Uuid.random().toString(),
    val timestamp: Instant = Clock.System.now(),
    val options: RpcOptions? = null,
) {
    companion object {
        fun get(
            url: String,
            meta: Inputs? = null,
            args: Inputs? = null,
            auth: Auth? = null,
        ): RpcRequest = RpcRequest(verb = Verb.Get, url = url, meta = meta, args = args, auth = auth)

        fun query(
            url: String,
            meta: Inputs? = null,
            args: Inputs? = null,
            auth: Auth? = null,
            data: Body? = null,
        ): RpcRequest = RpcRequest(verb = Verb.Query, url = url, meta = meta, args = args, data = data, auth = auth)

        fun create(
            url: String,
            meta: Inputs? = null,
            args: Inputs? = null,
            auth: Auth? = null,
            data: Body? = null,
        ): RpcRequest = RpcRequest(verb = Verb.Create, url = url, meta = meta, args = args, data = data, auth = auth)

        fun update(
            url: String,
            meta: Inputs? = null,
            args: Inputs? = null,
            auth: Auth? = null,
            data: Body? = null,
        ): RpcRequest = RpcRequest(verb = Verb.Update, url = url, meta = meta, args = args, data = data, auth = auth)

        fun patch(
            url: String,
            meta: Inputs? = null,
            args: Inputs? = null,
            auth: Auth? = null,
            data: Body? = null,
        ): RpcRequest = RpcRequest(verb = Verb.Patch, url = url, meta = meta, args = args, data = data, auth = auth)

        fun delete(
            url: String,
            meta: Inputs? = null,
            args: Inputs? = null,
            auth: Auth? = null,
        ): RpcRequest = RpcRequest(verb = Verb.Delete, url = url, meta = meta, args = args, auth = auth)
    }
}
