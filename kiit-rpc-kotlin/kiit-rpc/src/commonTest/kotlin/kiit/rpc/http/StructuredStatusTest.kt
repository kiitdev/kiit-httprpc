package kiit.rpc.http

import kiit.codes.Invalid
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.StatusConstants
import kiit.inputs.ListMap
import kiit.inputs.Meta
import kiit.inputs.MetaMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun metaOf(vararg pairs: Pair<String, String>): Meta = MetaMap(ListMap(pairs.toList()))

class StructuredStatusTest {
    @Test
    fun codeDetail_with_a_kiit_builtin_resolves_to_the_registry_singleton() {
        val body =
            """{"path":"${StatusConstants.KIIT}","code":"Failed:Restricted:DENIED",""" +
                """"success":false,"message":"The request was denied."}"""
        assertEquals(Restricted.DENIED, structuredStatusOrNull(body))
    }

    @Test
    fun codeDetail_with_a_custom_origin_constructs_the_matching_subtype_directly() {
        val body =
            """{"path":"stripe.com:payments.cards","code":"Failed:Rejected:DUPLICATE_CHARGE",""" +
                """"success":false,"message":"This charge has already been processed"}"""
        val expected = Rejected("DUPLICATE_CHARGE", "This charge has already been processed", "stripe.com", "payments.cards")
        assertEquals(expected, structuredStatusOrNull(body))
    }

    @Test
    fun problem_with_a_kiit_origin_type_resolves_via_its_embedded_code() {
        val body =
            """{"type":"https://www.kiit.dev/docs/kiit-codes?code=Failed:Invalid:INVALID_VALUE#taxonomy",""" +
                """"title":"The request had an invalid value.","status":400}"""
        assertEquals(Invalid.INVALID_VALUE, structuredStatusOrNull(body))
    }

    @Test
    fun problem_with_a_custom_origin_dash_path_is_not_reconstructed() {
        val body =
            """{"type":"https://stripe.com/errors/payments.cards/rejected/duplicate-charge",""" +
                """"title":"Duplicate charge","status":409}"""
        assertNull(structuredStatusOrNull(body))
    }

    @Test
    fun malformed_json_returns_null() {
        assertNull(structuredStatusOrNull("not json {"))
    }

    @Test
    fun a_json_array_is_not_an_object_and_returns_null() {
        assertNull(structuredStatusOrNull("[1,2,3]"))
    }

    @Test
    fun a_json_object_missing_required_fields_returns_null() {
        assertNull(structuredStatusOrNull("""{"code":"Failed:Invalid:INVALID_VALUE"}"""))
    }

    @Test
    fun header_with_a_custom_origin_decodes_scope_group_and_code_by_position() {
        val meta = metaOf("x-server-status-rfc9457" to "https://stripe.com/problems/payments.cards/rejected/duplicate-charge")
        val expected = Rejected("DUPLICATE_CHARGE", "", "stripe.com", "payments.cards")
        assertEquals(expected, headerStatusOrNull(meta))
    }

    @Test
    fun header_title_becomes_the_status_message() {
        val meta =
            metaOf(
                "x-server-status-rfc9457" to "https://stripe.com/problems/payments.cards/rejected/duplicate-charge",
                "x-server-status-rfc9457-title" to "This charge has already been processed",
            )
        val expected = Rejected("DUPLICATE_CHARGE", "This charge has already been processed", "stripe.com", "payments.cards")
        assertEquals(expected, headerStatusOrNull(meta))
    }

    @Test
    fun missing_header_returns_null() {
        assertNull(headerStatusOrNull(metaOf()))
    }

    @Test
    fun header_with_too_few_path_segments_returns_null() {
        assertNull(headerStatusOrNull(metaOf("x-server-status-rfc9457" to "https://stripe.com/rejected")))
    }
}
