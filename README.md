<div align="center">

# kiit-rpc

**A simple, declarative Kotlin Multiplatform client for RPC-style HTTP calls.**

Every call returns an `Outcome<HttpRpcResponse>` carrying a status that classifies the kind of
success or failure, instead of just a raw response object or a thrown exception. That status
comes from kiit-codes, the same taxonomy kiit-result already builds on. `get`, `query`, `create`,
`update`, `patch`, and `delete` all funnel through one pipeline: build the request, run it through
an optional chain of policies, resolve the status, hand back the outcome.

[![Build](https://img.shields.io/github/actions/workflow/status/kiitdev/kiit-rpc/ci.yml?branch=main)](https://github.com/kiitdev/kiit-rpc/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/kiitdev/kiit-rpc)](./LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-purple.svg)](https://kotlinlang.org)

Part of [Kiit](https://www.kiit.dev)

</div>

## Table of Contents

- [Why](#why)
- [Start](#start)
- [Concepts](#concepts)
- [Typed calls](#typed-calls)
- [Policies](#policies)
- [Usage](#usage)
- [Requirements](#requirements)
- [License](#license)

## Why

Most HTTP clients hand back either a response you inspect yourself or an exception you catch.
Neither tells you what *kind* of outcome you got, just that something happened. kiit-rpc pairs
every call with kiit-codes' status taxonomy: a 404 resolves to `Invalid.NOT_FOUND`, a 401 to
`Restricted.UNAUTHENTICATED`, and so on, whether the server is kiit-based (it can embed the exact
status in the response body) or any other API (the plain HTTP code maps to the closest match).

## Start

kiit-rpc hasn't been published to Maven Central yet. Once it is:

```kotlin
dependencies {
    implementation("dev.kiit:kiit-rpc:<version>")
}
```

`kiit-rpc` depends on `dev.kiit:kiit-codes` and `dev.kiit:kiit-result` transitively (both `api`),
so you don't need to add them separately.

**A basic call:**

```kotlin
import kiit.rpc.http.HttpRpc

val client = HttpRpc()
val outcome = client.get("https://httpbin.org/get", args = mapOf("q" to "hello"))

outcome.onSuccess { response -> println(response.body) }
    .onFailure { err -> println("failed: ${err.message}") }
```

See [`samples/sample-kotlin`](./samples/sample-kotlin) for a runnable end-to-end example,
including auth, a typed call, and a logging policy.

## Concepts

| Term | What it is |
|---|---|
| **`RpcClient`** | The public contract (`kiit.rpc`): `get`, `query`, `create`, `update`, `patch`, `delete`. `kiit.rpc.http.HttpRpc` is the Ktor-backed implementation. |
| **`HttpRpcResponse`** | Status code, headers, body. kiit-rpc's own type, Ktor's response type never leaks through. |
| **`Outcome<T>`** | `Result<T, Err>` from kiit-result. Every call returns `Outcome<HttpRpcResponse>`, carrying a resolved `Status`. |
| **`Policy<I, O>`** | Wraps a call: retry, log, rewrite the request, short-circuit. A list of them chains together, first one outermost. |
| **`Serializer<T>`** | Encode/decode abstraction behind typed calls. `KotlinxSerializer` is the default, bring your own for Moshi/Gson/Jackson. |
| **`StatusConverter`** (`kiit.rpc.http`) | Resolves the `Status` for a response. The default recognizes kiit's own structured error shapes first, falls back to the HTTP code otherwise. |

The public contract (`RpcClient`, `Body`, `Content`, `Auth`, `HttpRpcRequest`/`HttpRpcResponse`,
`Policy`, `Serializer`, `ExecuteParams`) lives at `kiit.rpc`. The concrete Ktor engine and its own
pluggable strategy objects (`HttpRpc`, `HttpRpcSettings`, `StatusConverter`) live at
`kiit.rpc.http`, so a future non-HTTP transport can sit alongside it as its own subpackage without
reshaping the public contract.

`query` is HTTP's newer method for a GET-like call that still carries a body, read-only like GET
but meant for searches too complex or too large for a URL. It's mapped to `POST` on the wire for
now, not the literal QUERY verb. OkHttp, the engine behind the JVM and Android targets, refuses to
build a GET request with a body at all, so QUERY-as-GET would fail there the moment a body is
actually present.

## Typed calls

`executeResult`/`executeOutcome` call and decode the response body into a type in one step:

```kotlin
import kiit.rpc.ExecuteParams
import kiit.rpc.HttpMethod
import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String)

val result = client.executeResult<User>(ExecuteParams(HttpMethod.Get, "https://api.example.com/users/1"))
```

`executeResult` returns a `Try<T>` (`Result<T, Throwable>`), `executeOutcome` returns an
`Outcome<T>` (`Result<T, Err>`) if you'd rather stay in kiit-codes' status world. Both have a
reified overload that infers the serializer for any `@Serializable` type, and a plain overload
taking an explicit `Serializer<T>` for anything else, including Swift, where reified generics
don't exist in the compiled framework.

Decoding only looks at the fields your type declares. A response with extra fields you don't care
about still decodes fine.

## Policies

A `Policy<HttpRpcRequest, HttpRpcResponse>` wraps every call before it reaches the network:

```kotlin
import kiit.rpc.HttpRpcRequest
import kiit.rpc.HttpRpcResponse
import kiit.rpc.Policy
import kiit.rpc.http.HttpRpc
import kiit.result.Outcome

class LoggingPolicy : Policy<HttpRpcRequest, HttpRpcResponse> {
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

val client = HttpRpc(policies = listOf(LoggingPolicy()))
```

The first policy in the list runs outermost. One that never calls `operation` short-circuits the
call entirely. That's on purpose, there's no guard against it: retry, auth refresh, and caching
all need that power.

## Usage

1. **Calling other kiit-based services.** The status comes back exact, not guessed from the HTTP
   code, since a kiit server can embed its own structured status in the response.
2. **Calling any other HTTP API.** The plain status code still maps to the closest kiit-codes
   status, the taxonomy isn't kiit-only.
3. **Typed responses.** `executeResult`/`executeOutcome` skip the manual decode step.
4. **Cross-cutting concerns.** Logging, retry, or auth refresh as a `Policy`, instead of wrapping
   every call site by hand.

**Good fit if:**
1. You already use (or want) kiit-codes' status taxonomy and want an HTTP client that plugs into
   it directly.
2. You want a request pipeline you can extend with policies, without forking the client.

**Probably not necessary if:**
1. You need HTTP features this doesn't cover, HTTP/2 push and WebSockets aren't in scope. Ktor
   itself supports them, this library just doesn't add an opinion on top.
2. You just need a plain HTTP call and don't want kiit-result/kiit-codes on your classpath at all.

## Requirements

- Kotlin Multiplatform
- JVM, Android, iOS (arm64, simulator arm64, x64)
- Depends on `dev.kiit:kiit-codes` and `dev.kiit:kiit-result` (transitively available to
  consumers via `api`)

## License

[Apache License 2.0](./LICENSE)

---

<div align="center">

**kiit-rpc** is one module of [Kiit](https://www.kiit.dev), a lightweight, modular Kotlin
toolkit for building server applications, APIs, CLIs, and jobs.

**Adopt one module at a time.**

</div>
