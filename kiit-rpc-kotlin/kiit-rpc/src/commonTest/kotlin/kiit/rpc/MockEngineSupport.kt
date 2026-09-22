package kiit.rpc

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** Builds an [HttpRpc] wired to a Ktor [MockEngine], so tests never make a real network call. */
fun mockHttpRpc(
    settings: HttpRpcSettings = HttpRpcSettings(),
    policies: List<HttpRpcPolicy> = emptyList(),
    handler: MockRequestHandler,
): HttpRpc = HttpRpc(settings = settings, policies = policies, engine = MockEngine(handler))

/** Shorthand for a JSON response — the common case across these tests. */
fun MockRequestHandleScope.respondJson(json: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(json, status, headersOf(HttpHeaders.ContentType, "application/json"))

/** The last [HttpRequestData.body]'s text, assuming [HttpRpc] sent it as [io.ktor.http.content.TextContent]. */
val HttpRequestData.textBody: String?
    get() = (body as? io.ktor.http.content.TextContent)?.text
