package kiit.rpc

import kiit.codes.Succeeded
import kiit.result.Outcome
import kiit.result.Success
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

private fun ok(value: String): Outcome<String> = Success(value, Succeeded.SUCCESS)

private fun tagging(tag: String): Policy<String, String> =
    object : Policy<String, String> {
        override suspend fun run(i: String, operation: suspend (String) -> Outcome<String>): Outcome<String> {
            val result = operation("$i>$tag")
            return result.map { "$it<$tag" }
        }
    }

class PolicyTest {
    @Test
    fun chain_with_no_policies_calls_last_directly() =
        runTest {
            val pipeline = Policies.chain(emptyList(), ::ok)
            assertEquals(ok("input"), pipeline("input"))
        }

    @Test
    fun chain_runs_first_policy_outermost() =
        runTest {
            // "first" wraps outermost: it appends its tag before "second" runs, and its own
            // wrapping (the "<first" suffix) is the last thing applied to the result.
            val pipeline = Policies.chain(listOf(tagging("first"), tagging("second")), ::ok)
            val result = pipeline("input")
            assertEquals(ok("input>first>second<second<first"), result)
        }

    @Test
    fun policy_can_short_circuit_without_calling_operation() =
        runTest {
            var called = false
            val shortCircuit =
                object : Policy<String, String> {
                    override suspend fun run(i: String, operation: suspend (String) -> Outcome<String>): Outcome<String> {
                        return ok("short-circuited")
                    }
                }
            val pipeline =
                Policies.chain(listOf(shortCircuit)) { input ->
                    called = true
                    ok(input)
                }
            val result = pipeline("input")
            assertFalse(called)
            assertEquals(ok("short-circuited"), result)
        }

    @Test
    fun policy_can_rewrite_input_before_calling_next() =
        runTest {
            val rewriter =
                object : Policy<String, String> {
                    override suspend fun run(i: String, operation: suspend (String) -> Outcome<String>): Outcome<String> {
                        return operation("rewritten")
                    }
                }
            val pipeline = Policies.chain(listOf(rewriter), ::ok)
            val result = assertIs<Success<String>>(pipeline("original"))
            assertEquals("rewritten", result.value)
        }
}
