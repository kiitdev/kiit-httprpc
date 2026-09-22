package kiit.rpc

import kiit.codes.Err
import kiit.codes.Unserved
import kiit.result.Failure
import kiit.result.Outcome
import kiit.result.Result
import kiit.result.Success
import kiit.result.Try
import kotlinx.coroutines.CancellationException

/**
 * A typed call's arguments, bundled so `executeOutcome`/`executeResult` take one parameter
 * instead of the six [RpcClient]'s own per-verb methods spread across their signature. [method]
 * picks which [RpcClient] method actually runs, see [dispatch].
 */
data class ExecuteParams(
    val method: HttpMethod,
    val url: String,
    val meta: Meta? = null,
    val args: Args? = null,
    val auth: Auth? = null,
    val body: Body? = null,
)

/** Dispatches to the [RpcClient] method matching [ExecuteParams.method]. [HttpMethod] picks the verb. */
private suspend fun RpcClient.dispatch(params: ExecuteParams): Outcome<HttpRpcResponse> =
    with(params) {
        when (method) {
            HttpMethod.Get -> get(url, meta, args, auth)
            HttpMethod.Query -> query(url, meta, args, auth, body)
            HttpMethod.Post -> create(url, meta, args, auth, body)
            HttpMethod.Put -> update(url, meta, args, auth, body)
            HttpMethod.Patch -> patch(url, meta, args, auth, body)
            HttpMethod.Delete -> delete(url, meta, args, auth, body)
        }
    }

/**
 * Decodes a successful response's body via [serializer], folding a decode failure into the
 * error branch the same way a transport failure would be. A [Failure] passes through as-is,
 * `Outcome<HttpRpcResponse>`'s `Nothing` success type already makes it an `Outcome<T>` for any T.
 */
private suspend fun <T> Outcome<HttpRpcResponse>.decode(serializer: Serializer<T>): Outcome<T> =
    when (this) {
        is Success -> decodeBody(this, serializer)
        is Failure -> this
    }

/**
 * A custom [Serializer] can throw anything (`SerializationException`, `IllegalArgumentException`,
 * whatever the underlying JSON library uses), so a broad catch is deliberate here, same reasoning
 * as [HttpRpc]'s own network-call catch. [CancellationException] is excluded first.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> decodeBody(success: Success<HttpRpcResponse>, serializer: Serializer<T>): Outcome<T> =
    try {
        Success(serializer.decode(success.value.body), success.status)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Failure(Err.ex(e), Unserved.UNEXPECTED)
    }

/** Runs [params] and decodes the response body into [T] via [serializer]. */
suspend fun <T> RpcClient.executeOutcome(params: ExecuteParams, serializer: Serializer<T>): Outcome<T> {
    return dispatch(params).decode(serializer)
}

/** Same as [executeOutcome], but the error branch is a plain [Throwable] ([Try]), via [Result.toTry]. */
suspend fun <T> RpcClient.executeResult(params: ExecuteParams, serializer: Serializer<T>): Try<T> =
    executeOutcome(params, serializer).toTry()

/**
 * Reified convenience for the common `@Serializable` case. Kotlin-only: `reified` inline
 * functions never exist in the compiled framework, so Swift can't call this. Swift callers use
 * the plain [executeOutcome] overload with an explicit `KotlinxSerializer(T.serializer())`.
 */
suspend inline fun <reified T> RpcClient.executeOutcome(params: ExecuteParams): Outcome<T> =
    executeOutcome(params, serializer = Serializer())

/** Reified convenience for [executeResult], see [executeOutcome]'s reified overload. */
suspend inline fun <reified T> RpcClient.executeResult(params: ExecuteParams): Try<T> {
    return executeOutcome<T>(params).toTry()
}
