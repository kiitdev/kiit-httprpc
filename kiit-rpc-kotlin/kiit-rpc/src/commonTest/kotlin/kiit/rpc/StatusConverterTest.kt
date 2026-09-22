package kiit.rpc

import kiit.codes.Invalid
import kiit.codes.Restricted
import kiit.codes.Succeeded
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CODE_DETAIL_DENIED =
    """{"path":"kiit.dev","code":"Failed:Restricted:DENIED","success":false,"message":"The request was denied."}"""

class StatusConverterTest {
    @Test
    fun prefers_a_codeDetail_body_over_the_raw_http_status() {
        val response =
            HttpRpcResponse(status = 403, headers = mapOf("Content-Type" to "application/json"), body = CODE_DETAIL_DENIED)
        assertEquals(Restricted.DENIED, KiitStatusConverter.convert(response))
    }

    @Test
    fun ignores_a_structured_looking_body_when_content_type_is_not_json() {
        val response =
            HttpRpcResponse(status = 403, headers = mapOf("Content-Type" to "text/html"), body = CODE_DETAIL_DENIED)
        assertEquals(Restricted.FORBIDDEN, KiitStatusConverter.convert(response))
    }

    @Test
    fun falls_back_to_the_http_code_when_the_json_body_is_not_structured() {
        val response =
            HttpRpcResponse(status = 404, headers = mapOf("Content-Type" to "application/json"), body = """{"error":"nope"}""")
        assertEquals(Invalid.NOT_FOUND, KiitStatusConverter.convert(response))
    }

    @Test
    fun falls_back_to_the_http_code_when_the_body_is_blank() {
        val response = HttpRpcResponse(status = 200, headers = emptyMap(), body = "")
        assertEquals(Succeeded.SUCCESS, KiitStatusConverter.convert(response))
    }
}
