package kiit.rpc.http

import kiit.codes.Invalid
import kiit.codes.Restricted
import kiit.codes.Succeeded
import kiit.codes.Unserved
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpStatusCodesTest {
    @Test
    fun maps_specific_codes_to_their_own_canonical_status() {
        assertEquals(Succeeded.SUCCESS, 200.toStatus())
        assertEquals(Succeeded.CREATED, 201.toStatus())
        assertEquals(Restricted.UNAUTHENTICATED, 401.toStatus())
        assertEquals(Restricted.FORBIDDEN, 403.toStatus())
        assertEquals(Invalid.NOT_FOUND, 404.toStatus())
        assertEquals(Unserved.RATE_LIMITED, 429.toStatus())
        assertEquals(Unserved.UNEXPECTED, 500.toStatus())
        assertEquals(Unserved.UNDER_MAINTENANCE, 503.toStatus())
    }

    @Test
    fun falls_back_to_the_group_default_for_an_unmapped_code_in_range() {
        // 418 isn't in the specific table, but is a 4xx, so it lands on the generic Invalid bucket.
        assertEquals(Invalid.INVALID_VALUE, 418.toStatus())
    }

    @Test
    fun falls_back_to_unexpected_for_a_code_outside_every_range() {
        assertEquals(Unserved.UNEXPECTED, 999.toStatus())
    }
}
