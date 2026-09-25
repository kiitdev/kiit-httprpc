package kiit.rpc.http

import kiit.call.Contents
import kiit.codes.Invalid
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.StatusConstants
import kiit.codes.Succeeded
import kiit.inputs.ListMap
import kiit.inputs.Meta
import kiit.inputs.MetaMap
import kiit.rpc.RpcResponse
import kotlin.test.Test
import kotlin.test.assertEquals

private val CODE_DETAIL_DENIED =
    """{"path":"${StatusConstants.KIIT}","code":"Failed:Restricted:DENIED",""" +
        """"success":false,"message":"The request was denied."}"""

private fun metaOf(vararg pairs: Pair<String, String>): Meta = MetaMap(ListMap(pairs.toList()))

class StatusConverterTest {
    private val converter = KiitStatusConverter(parseStatusFromBody = true)

    @Test
    fun prefers_a_codeDetail_body_over_the_raw_http_status() {
        val response = RpcResponse(status = 403, data = Contents.json(CODE_DETAIL_DENIED))
        assertEquals(Restricted.DENIED, converter.convert(response))
    }

    @Test
    fun falls_back_when_the_body_is_not_valid_json() {
        val response = RpcResponse(status = 403, data = Contents.text("<html>Forbidden</html>"))
        assertEquals(Restricted.FORBIDDEN, converter.convert(response))
    }

    @Test
    fun falls_back_to_the_http_code_when_the_json_body_is_not_structured() {
        val response = RpcResponse(status = 404, data = Contents.json("""{"error":"nope"}"""))
        assertEquals(Invalid.NOT_FOUND, converter.convert(response))
    }

    @Test
    fun falls_back_to_the_http_code_when_the_body_is_blank() {
        val response = RpcResponse(status = 200, data = Contents.text(""))
        assertEquals(Succeeded.SUCCESS, converter.convert(response))
    }

    @Test
    fun body_parsing_is_off_by_default() {
        val response = RpcResponse(status = 403, data = Contents.json(CODE_DETAIL_DENIED))
        assertEquals(Restricted.FORBIDDEN, KiitStatusConverter().convert(response))
    }

    @Test
    fun the_status_header_wins_even_when_body_parsing_is_enabled() {
        val meta = metaOf("x-server-status-rfc9457" to "https://stripe.com/problems/payments.cards/rejected/duplicate-charge")
        val response = RpcResponse(status = 409, data = Contents.json(CODE_DETAIL_DENIED), meta = meta)
        assertEquals(Rejected("DUPLICATE_CHARGE", "", "stripe.com", "payments.cards"), converter.convert(response))
    }

    @Test
    fun the_status_header_is_checked_even_when_body_parsing_is_off() {
        val meta = metaOf("x-server-status-rfc9457" to "https://stripe.com/problems/payments.cards/rejected/duplicate-charge")
        val response = RpcResponse(status = 409, data = Contents.text(""), meta = meta)
        assertEquals(Rejected("DUPLICATE_CHARGE", "", "stripe.com", "payments.cards"), KiitStatusConverter().convert(response))
    }
}
