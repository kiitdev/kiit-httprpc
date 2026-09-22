package sample

import kiit.rpc.Auth
import kiit.rpc.Body
import kiit.rpc.ExecuteParams
import kiit.rpc.HttpMethod
import kiit.rpc.HttpRpc
import kiit.rpc.HttpRpcRequest
import kiit.rpc.HttpRpcResponse
import kiit.rpc.Policy
import kiit.rpc.executeResult
import kiit.result.Outcome
import kiit.result.Success
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

/**
 * Canonical sample for kiit-rpc. One running example against httpbin.org, in four parts:
 * 1. Basic calls, 2. Auth, 3. Typed decode, 4. Policy.
 *
 * Every example is wrapped in `// <example id="..." tags="...">` ... `// </example>` so it can be
 * extracted for the docs later, matching kiit-codes' sample. The `verify(...)` calls sit outside
 * the markers, they check the example still works against the real httpbin.org.
 */

private var checks = 0

private fun verify(label: String, condition: Boolean) {
    check(condition) { "FAILED: $label" }
    checks++
    println("  ok: $label")
}

private fun section(title: String) {
    println()
    println("=".repeat(60))
    println(title)
    println("=".repeat(60))
}

// ============================================================
// Part 1: Basic calls
// ============================================================

private suspend fun showBasicCalls(client: HttpRpc) {
    section("Part 1: Basic calls")

    // <example id="basic-get" tags="calls">
    // A GET with query params, no body. args become the URL's query string.
    val getOutcome = client.get("https://httpbin.org/get", args = mapOf("greeting" to "hello"))
    // </example>
    verify("basic-get: succeeded", getOutcome is Success)

    // <example id="basic-create" tags="calls">
    // create (POST) with a JSON body.
    val createOutcome = client.create("https://httpbin.org/post", body = Body.JsonContent("""{"name":"Ada"}"""))
    // </example>
    verify("basic-create: succeeded", createOutcome is Success)
}

// ============================================================
// Part 2: Auth
// ============================================================

private suspend fun showAuth(client: HttpRpc) {
    section("Part 2: Auth")

    // <example id="auth-bearer" tags="auth">
    // httpbin's /bearer endpoint echoes back the token when the Authorization header is valid.
    val outcome = client.get("https://httpbin.org/bearer", auth = Auth.Bearer("demo-token"))
    // </example>
    verify("auth-bearer: succeeded", outcome is Success)
    if (outcome is Success) {
        verify("auth-bearer: token echoed back", outcome.value.body.contains("demo-token"))
    }
}

// ============================================================
// Part 3: Typed decode
// ============================================================

@Serializable
private data class HttpBinGet(val url: String)

private suspend fun showTypedDecode(client: HttpRpc) {
    section("Part 3: Typed decode")

    // <example id="typed-executeResult" tags="typed">
    // executeResult<T> calls and decodes the body into T in one step.
    val result = client.executeResult<HttpBinGet>(ExecuteParams(HttpMethod.Get, "https://httpbin.org/get"))
    // </example>
    result.onSuccess { println("decoded url: ${it.url}") }
    verify("typed-executeResult: decoded", result.getOrNull()?.url?.contains("httpbin.org/get") == true)
}

// ============================================================
// Part 4: Policy
// ============================================================

// <example id="policy-logging" tags="policy">
// A Policy that logs before and after the call it wraps.
private class LoggingPolicy : Policy<HttpRpcRequest, HttpRpcResponse> {
    override suspend fun run(
        i: HttpRpcRequest,
        operation: suspend (HttpRpcRequest) -> Outcome<HttpRpcResponse>,
    ): Outcome<HttpRpcResponse> {
        println("-> ${i.method} ${i.url}")
        val outcome = operation(i)
        println("<- ${outcome.status.name}")
        return outcome
    }
}
// </example>

private suspend fun showPolicy() {
    section("Part 4: Policy")

    // <example id="policy-attach" tags="policy">
    // Attach it via the policies list when constructing HttpRpc.
    val client = HttpRpc(policies = listOf(LoggingPolicy()))
    val outcome = client.get("https://httpbin.org/get")
    // </example>
    verify("policy-attach: succeeded", outcome is Success)
}

fun main() =
    runBlocking {
        val client = HttpRpc()
        showBasicCalls(client)
        showAuth(client)
        showTypedDecode(client)
        showPolicy()

        println()
        println("All $checks checks passed.")
    }
