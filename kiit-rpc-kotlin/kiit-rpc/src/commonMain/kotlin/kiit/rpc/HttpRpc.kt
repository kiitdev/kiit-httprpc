package kiit.rpc

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.content.TextContent
import io.ktor.http.encodeURLParameter
import kiit.codes.Err
import kiit.codes.Failed
import kiit.codes.Passed
import kiit.codes.Unserved
import kiit.result.Failure
import kiit.result.Outcome
import kiit.result.Success
import kotlinx.coroutines.CancellationException
import kotlin.io.encoding.Base64
import io.ktor.http.HttpMethod as KtorHttpMethod

/** Bundles a call's arguments into one value, instead of six loose parameters threaded through [HttpRpc]. */
private data class CallParams(
    val method: HttpMethod,
    val url: String,
    val meta: Meta?,
    val args: Args?,
    val auth: Auth?,
    val body: Body?,
)

/**
 * Ktor-backed [RpcClient]. Every call funnels through [call], the one place the [Policy] chain
 * runs, the actual network call happens (in [performCall]), and the response's
 * [kiit.codes.Status] gets resolved via [statusConverter].
 */
class HttpRpc(
    private val settings: HttpRpcSettings = HttpRpcSettings(),
    private val policies: List<HttpRpcPolicy> = emptyList(),
    private val statusConverter: StatusConverter = KiitStatusConverter,
    private val engine: HttpClientEngine? = null,
) : RpcClient {
    private val client: HttpClient by lazy { buildClient() }

    private fun buildClient(): HttpClient {
        val customEngine = engine
        return if (customEngine != null) {
            HttpClient(customEngine) { applyTimeoutAndRedirects() }
        } else {
            HttpClient { applyTimeoutAndRedirects() }
        }
    }

    private fun HttpClientConfig<*>.applyTimeoutAndRedirects() {
        install(HttpTimeout) {
            requestTimeoutMillis = settings.requestTimeoutMillis
            connectTimeoutMillis = settings.connectTimeoutMillis
            socketTimeoutMillis = settings.socketTimeoutMillis
        }
        followRedirects = settings.followRedirects
    }

    override suspend fun get(
        url: String,
        meta: Meta?,
        args: Args?,
        auth: Auth?,
    ): Outcome<HttpRpcResponse> = call(CallParams(HttpMethod.Get, url, meta, args, auth, null))

    override suspend fun query(
        url: String,
        meta: Meta?,
        args: Args?,
        auth: Auth?,
        body: Body?,
    ): Outcome<HttpRpcResponse> = call(CallParams(HttpMethod.Query, url, meta, args, auth, body))

    override suspend fun create(
        url: String,
        meta: Meta?,
        args: Args?,
        auth: Auth?,
        body: Body?,
    ): Outcome<HttpRpcResponse> = call(CallParams(HttpMethod.Post, url, meta, args, auth, body))

    override suspend fun update(
        url: String,
        meta: Meta?,
        args: Args?,
        auth: Auth?,
        body: Body?,
    ): Outcome<HttpRpcResponse> = call(CallParams(HttpMethod.Put, url, meta, args, auth, body))

    override suspend fun patch(
        url: String,
        meta: Meta?,
        args: Args?,
        auth: Auth?,
        body: Body?,
    ): Outcome<HttpRpcResponse> = call(CallParams(HttpMethod.Patch, url, meta, args, auth, body))

    override suspend fun delete(
        url: String,
        meta: Meta?,
        args: Args?,
        auth: Auth?,
        body: Body?,
    ): Outcome<HttpRpcResponse> = call(CallParams(HttpMethod.Delete, url, meta, args, auth, body))

    private suspend fun call(params: CallParams): Outcome<HttpRpcResponse> {
        val effectiveBody = if (params.method == HttpMethod.Get) null else params.body
        val request = buildRequest(params, effectiveBody)
        val multipart = effectiveBody as? Body.MultiPart
        val pipeline = Policies.chain(policies) { req -> performCall(req, multipart) }
        return pipeline(request)
    }

    private fun buildRequest(params: CallParams, effectiveBody: Body?): HttpRpcRequest =
        HttpRpcRequest(
            method = params.method,
            url = buildUrl(params.url, params.args),
            headers = buildHeaders(params.meta, params.auth, effectiveBody),
            body = resolveBodyText(effectiveBody),
        )

    private fun buildUrl(url: String, args: Args?): String {
        if (args.isNullOrEmpty()) return url
        val builder = URLBuilder(url)
        args.forEach { (key, value) -> builder.parameters.append(key, value) }
        return builder.buildString()
    }

    private fun buildHeaders(meta: Meta?, auth: Auth?, body: Body?): Map<String, String> {
        val headers = LinkedHashMap<String, String>()
        headers.putAll(settings.defaultHeaders)
        meta?.let { headers.putAll(it) }
        contentTypeFor(body)?.let { headers[HttpHeaders.ContentType] = it }
        authHeader(auth)?.let { (key, value) -> headers[key] = value }
        return headers
    }

    /** Multipart's Content-Type (with boundary) is set by Ktor itself, see [buildMultiPartBody]. */
    private fun contentTypeFor(body: Body?): String? =
        when (body) {
            null, is Body.MultiPart -> null
            is Body.FormData -> "application/x-www-form-urlencoded"
            is Body.RawContent -> "text/plain"
            is Body.JsonContent -> "application/json"
        }

    private fun authHeader(auth: Auth?): Pair<String, String>? =
        when (auth) {
            null -> null
            is Auth.Basic -> HttpHeaders.Authorization to basicAuthValue(auth)
            is Auth.Bearer -> HttpHeaders.Authorization to "Bearer ${auth.token}"
        }

    private fun basicAuthValue(auth: Auth.Basic): String {
        val credentials = "${auth.name}:${auth.pswd}".encodeToByteArray()
        return "Basic " + Base64.Default.encode(credentials)
    }

    /** [Policy] can only see/rewrite this string, so [Body.MultiPart] gets a placeholder, not its actual bytes. */
    private fun resolveBodyText(body: Body?): String? =
        when (body) {
            null -> null
            is Body.MultiPart -> "<multipart: ${body.values.size} part(s)>"
            is Body.FormData -> encodeFormData(body.values)
            is Body.RawContent -> body.content
            is Body.JsonContent -> body.content
        }

    private fun encodeFormData(values: List<Pair<String, String>>): String =
        values.joinToString("&") { (key, value) -> "${key.encodeURLParameter()}=${value.encodeURLParameter()}" }

    /**
     * Different Ktor engines throw different exception types for the same conceptual failure
     * (connection refused, DNS failure, TLS error), so a broad catch is the correct choice here,
     * not an oversight. [CancellationException] is excluded first, so cancelling the caller's
     * coroutine is never mistaken for a failed call.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun performCall(request: HttpRpcRequest, multipart: Body.MultiPart?): Outcome<HttpRpcResponse> =
        try {
            val response =
                client.request(request.url) {
                    method = request.method.toKtorMethod()
                    request.headers.forEach { (key, value) -> header(key, value) }
                    applyBody(request, multipart)
                }
            toOutcome(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Failure(Err.ex(e), Unserved.UNEXPECTED)
        }

    /** [multipart] takes precedence: it carries the real bytes/boundary, [request].body is just the placeholder. */
    private fun HttpRequestBuilder.applyBody(request: HttpRpcRequest, multipart: Body.MultiPart?) {
        when {
            multipart != null -> setBody(buildMultiPartBody(multipart))
            request.body != null -> setBody(TextContent(request.body, contentTypeOf(request)))
        }
    }

    private fun contentTypeOf(request: HttpRpcRequest): ContentType =
        request.headers[HttpHeaders.ContentType]?.let(ContentType::parse) ?: ContentType.Text.Plain

    private fun buildMultiPartBody(multipart: Body.MultiPart): MultiPartFormDataContent =
        MultiPartFormDataContent(
            formData {
                multipart.values.forEach { (name, content) -> appendPart(name, content) }
            },
        )

    private fun FormBuilder.appendPart(name: String, content: Content) {
        when (content) {
            is ContentText -> append(name, content.text)
            is ContentFile -> appendFilePart(name, content)
        }
    }

    private fun FormBuilder.appendFilePart(name: String, content: ContentFile) {
        val partHeaders =
            Headers.build {
                append(HttpHeaders.ContentType, content.type.http)
                append(HttpHeaders.ContentDisposition, "filename=\"${content.name}\"")
            }
        append(name, content.data, partHeaders)
    }

    private suspend fun toOutcome(response: HttpResponse): Outcome<HttpRpcResponse> {
        val httpRpcResponse =
            HttpRpcResponse(
                status = response.status.value,
                headers = response.headers.entries().associate { it.key to (it.value.firstOrNull() ?: "") },
                body = response.bodyAsText(),
            )
        return when (val status = statusConverter.convert(httpRpcResponse)) {
            is Passed -> Success(httpRpcResponse, status)
            is Failed -> Failure(Err.of(status), status)
        }
    }

    /**
     * QUERY maps to POST on the wire, not the literal QUERY verb, for now. OkHttp (our
     * JVM/Android engine) throws building a GET request with a body at all, so mapping to GET
     * would crash there whenever [Body] is actually present. POST permits a body on every engine
     * and needs no server-side QUERY support, at the cost of QUERY's safe/idempotent-like-GET
     * semantic.
     */
    private fun HttpMethod.toKtorMethod(): KtorHttpMethod =
        when (this) {
            HttpMethod.Get -> KtorHttpMethod.Get
            HttpMethod.Query -> KtorHttpMethod.Post
            HttpMethod.Post -> KtorHttpMethod.Post
            HttpMethod.Put -> KtorHttpMethod.Put
            HttpMethod.Patch -> KtorHttpMethod.Patch
            HttpMethod.Delete -> KtorHttpMethod.Delete
        }
}
