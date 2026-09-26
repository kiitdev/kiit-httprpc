package kiit.rpc

import kiit.codes.Err
import kiit.codes.Unserved
import kiit.requests.Contents
import kiit.result.Failure
import kiit.result.Outcome
import kiit.result.Result
import kiit.result.Success
import kiit.result.Try
import kotlinx.coroutines.CancellationException

/**
 * Decodes a successful response's body via [serializer], folding a decode failure into the
 * error branch the same way a transport failure would be. A [Failure] passes through as-is,
 * `Outcome<RpcResponse>`'s `Nothing` success type already makes it an `Outcome<T>` for any T.
 */
private suspend fun <T> Outcome<RpcResponse>.decode(serializer: Serializer<T>): Outcome<T> =
    when (this) {
        is Success -> decodeBody(this, serializer)
        is Failure -> this
    }

/**
 * A custom [Serializer] can throw anything (`SerializationException`, `IllegalArgumentException`,
 * whatever the underlying JSON library uses), so a broad catch is deliberate here, same reasoning
 * as [kiit.rpc.http.HttpRpc]'s own network-call catch. [CancellationException] is excluded first.
 */
@Suppress("TooGenericExceptionCaught")
private fun <T> decodeBody(success: Success<RpcResponse>, serializer: Serializer<T>): Outcome<T> =
    try {
        val text = Contents.toText(success.value.data) ?: ""
        Success(serializer.decode(text), success.status)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Failure(Err.ex(e), Unserved.UNEXPECTED)
    }

/** Runs [request] and decodes the response body into [T] via [serializer]. */
suspend fun <T> RpcClient.executeOutcome(request: RpcRequest, serializer: Serializer<T>): Outcome<T> {
    return execute(request).decode(serializer)
}

/** Same as [executeOutcome], but the error branch is a plain [Throwable] ([Try]), via [Result.toTry]. */
suspend fun <T> RpcClient.executeResult(request: RpcRequest, serializer: Serializer<T>): Try<T> =
    executeOutcome(request, serializer).toTry()

/**
 * Reified convenience for the common `@Serializable` case. Kotlin-only: `reified` inline
 * functions never exist in the compiled framework, so Swift can't call this. Swift callers use
 * the plain [executeOutcome] overload with an explicit `KotlinxSerializer(T.serializer())`.
 */
suspend inline fun <reified T> RpcClient.executeOutcome(request: RpcRequest): Outcome<T> =
    executeOutcome(request, serializer = Serializer())

/** Reified convenience for [executeResult], see [executeOutcome]'s reified overload. */
suspend inline fun <reified T> RpcClient.executeResult(request: RpcRequest): Try<T> {
    return executeOutcome<T>(request).toTry()
}
