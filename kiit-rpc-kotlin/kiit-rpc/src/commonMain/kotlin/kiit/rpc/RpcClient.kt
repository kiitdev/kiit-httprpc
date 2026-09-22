package kiit.rpc

import kiit.result.Outcome

/**
 * Public contract [HttpRpc] implements. Lets a consumer mock or substitute the client in their
 * own tests without depending on the concrete Ktor-backed implementation.
 */
interface RpcClient {
    suspend fun get(
        url: String,
        meta: Meta? = null,
        args: Args? = null,
        auth: Auth? = null,
    ): Outcome<HttpRpcResponse>

    suspend fun query(
        url: String,
        meta: Meta? = null,
        args: Args? = null,
        auth: Auth? = null,
        body: Body? = null,
    ): Outcome<HttpRpcResponse>

    suspend fun create(
        url: String,
        meta: Meta? = null,
        args: Args? = null,
        auth: Auth? = null,
        body: Body? = null,
    ): Outcome<HttpRpcResponse>

    suspend fun update(
        url: String,
        meta: Meta? = null,
        args: Args? = null,
        auth: Auth? = null,
        body: Body? = null,
    ): Outcome<HttpRpcResponse>

    suspend fun patch(
        url: String,
        meta: Meta? = null,
        args: Args? = null,
        auth: Auth? = null,
        body: Body? = null,
    ): Outcome<HttpRpcResponse>

    suspend fun delete(
        url: String,
        meta: Meta? = null,
        args: Args? = null,
        auth: Auth? = null,
        body: Body? = null,
    ): Outcome<HttpRpcResponse>
}
