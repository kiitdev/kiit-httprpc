package kiit.rpc

import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kiit.codes.Invalid
import kiit.codes.Succeeded
import kiit.result.Failure
import kiit.result.Success
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import io.ktor.http.HttpMethod as KtorHttpMethod

private const val BASE_URL = "https://api.example.com/users"

class HttpRpcTest {
    @Test
    fun get_sends_query_params_from_args_and_no_body() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.get(BASE_URL, args = mapOf("page" to "2", "size" to "10"))

            assertEquals(KtorHttpMethod.Get, captured.method)
            assertEquals("2", captured.url.parameters["page"])
            assertEquals("10", captured.url.parameters["size"])
            assertEquals(null, captured.textBody)
        }

    @Test
    fun create_sends_post_with_json_content_type_and_body() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.create(BASE_URL, body = Body.JsonContent("""{"name":"Ada"}"""))

            assertEquals(KtorHttpMethod.Post, captured.method)
            assertEquals("application/json", captured.headers["Content-Type"])
            assertEquals("""{"name":"Ada"}""", captured.textBody)
        }

    @Test
    fun update_sends_put_and_patch_sends_patch_and_delete_sends_delete() =
        runTest {
            val methods = mutableListOf<KtorHttpMethod>()
            val client = mockHttpRpc { request ->
                methods += request.method
                respond("", HttpStatusCode.OK)
            }
            client.update(BASE_URL, body = Body.RawContent("x"))
            client.patch(BASE_URL, body = Body.RawContent("x"))
            client.delete(BASE_URL)

            assertEquals(listOf(KtorHttpMethod.Put, KtorHttpMethod.Patch, KtorHttpMethod.Delete), methods)
        }

    @Test
    fun query_sends_post_on_the_wire_with_its_body_attached() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.query(BASE_URL, body = Body.JsonContent("""{"filter":"active"}"""))

            assertEquals(KtorHttpMethod.Post, captured.method)
            assertEquals("""{"filter":"active"}""", captured.textBody)
        }

    @Test
    fun meta_headers_merge_with_default_headers_and_override_on_conflict() =
        runTest {
            lateinit var captured: HttpRequestData
            val settings = HttpRpcSettings(defaultHeaders = mapOf("X-Client" to "kiit-rpc", "X-Env" to "prod"))
            val client = mockHttpRpc(settings = settings) { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.get(BASE_URL, meta = mapOf("X-Env" to "staging"))

            assertEquals("kiit-rpc", captured.headers["X-Client"])
            assertEquals("staging", captured.headers["X-Env"])
        }

    @Test
    fun basic_auth_produces_a_base64_authorization_header() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.get(BASE_URL, auth = Auth.Basic("user", "pass"))

            // "user:pass" base64-encoded, verified against the known constant rather than re-deriving it.
            assertEquals("Basic dXNlcjpwYXNz", captured.headers["Authorization"])
        }

    @Test
    fun bearer_auth_produces_a_bearer_authorization_header() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.get(BASE_URL, auth = Auth.Bearer("token123"))

            assertEquals("Bearer token123", captured.headers["Authorization"])
        }

    @Test
    fun form_data_body_is_url_encoded_with_the_right_content_type() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            client.create(BASE_URL, body = Body.FormData(listOf("a" to "1", "b" to "hello world")))

            assertEquals("application/x-www-form-urlencoded", captured.headers["Content-Type"])
            assertEquals("a=1&b=hello+world", captured.textBody)
        }

    @Test
    fun multipart_body_gets_a_multipart_content_type_with_a_boundary() =
        runTest {
            lateinit var captured: HttpRequestData
            val client = mockHttpRpc { request ->
                captured = request
                respond("", HttpStatusCode.OK)
            }
            val multipart = Body.MultiPart(listOf("note" to ContentText("hello")))
            client.create(BASE_URL, body = multipart)

            assertTrue(captured.body.contentType.toString().startsWith("multipart/form-data"))
        }

    @Test
    fun a_2xx_response_with_no_structured_body_resolves_via_the_http_code_table() =
        runTest {
            val client = mockHttpRpc { respond("", HttpStatusCode.OK) }
            val outcome = client.get(BASE_URL)

            val success = assertIs<Success<HttpRpcResponse>>(outcome)
            assertEquals(Succeeded.SUCCESS, success.status)
        }

    @Test
    fun a_404_response_resolves_to_a_failure_with_not_found() =
        runTest {
            val client = mockHttpRpc { respond("", HttpStatusCode.NotFound) }
            val outcome = client.get(BASE_URL)

            val failure = assertIs<Failure<*>>(outcome)
            assertEquals(Invalid.NOT_FOUND, failure.status)
        }

    @Test
    fun a_request_that_exceeds_the_configured_timeout_fails() =
        runTest {
            val settings = HttpRpcSettings(requestTimeoutMillis = 20)
            val client =
                mockHttpRpc(settings = settings) {
                    delay(200)
                    respond("", HttpStatusCode.OK)
                }
            val outcome = client.get(BASE_URL)

            assertIs<Failure<*>>(outcome)
        }
}
