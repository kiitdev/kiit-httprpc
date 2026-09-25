package kiit.rpc

import kiit.inputs.Inputs
import kiit.result.Outcome

/**
 * Public contract [kiit.rpc.http.HttpRpc] implements, so a consumer can mock or substitute the
 * client in tests without depending on the concrete Ktor implementation.
 *
 * [execute] is the only abstract method. Every named verb below is a default built on top of it,
 * so a new transport only has to implement [execute] to get the whole named-method API for free.
 */
interface RpcClient {
    suspend fun execute(request: RpcRequest): Outcome<RpcResponse>

    suspend fun get(url: String, meta: Inputs? = null, args: Inputs? = null, auth: Auth? = null): Outcome<RpcResponse> =
        execute(RpcRequest.get(url, meta, args, auth))

    suspend fun query(
        url: String,
        meta: Inputs? = null,
        args: Inputs? = null,
        auth: Auth? = null,
        data: Body? = null,
    ): Outcome<RpcResponse> = execute(RpcRequest.query(url, meta, args, auth, data))

    suspend fun create(
        url: String,
        meta: Inputs? = null,
        args: Inputs? = null,
        auth: Auth? = null,
        data: Body? = null,
    ): Outcome<RpcResponse> = execute(RpcRequest.create(url, meta, args, auth, data))

    suspend fun update(
        url: String,
        meta: Inputs? = null,
        args: Inputs? = null,
        auth: Auth? = null,
        data: Body? = null,
    ): Outcome<RpcResponse> = execute(RpcRequest.update(url, meta, args, auth, data))

    suspend fun patch(
        url: String,
        meta: Inputs? = null,
        args: Inputs? = null,
        auth: Auth? = null,
        data: Body? = null,
    ): Outcome<RpcResponse> = execute(RpcRequest.patch(url, meta, args, auth, data))

    suspend fun delete(url: String, meta: Inputs? = null, args: Inputs? = null, auth: Auth? = null): Outcome<RpcResponse> =
        execute(RpcRequest.delete(url, meta, args, auth))
}
