package kiit.rpc

import kiit.result.Outcome

/**
 * Mirrors `kiit.policy`'s `Policy<I, O>` shape (`kiit/src/internal/policy`, unpublished/JVM-only
 * in the monorepo), reimplemented locally since that module isn't published anywhere yet.
 * Swappable for a real dependency later if `kiit-policy` gets extracted into its own repo.
 *
 * A [Policy] wraps [operation]. It decides whether and how to call it: retry, log around it,
 * rewrite [i] first, or skip it entirely, and must produce an [Outcome].
 *
 * @param I input type
 * @param O output type
 */
interface Policy<I, O> {
    suspend fun run(i: I, operation: suspend (I) -> Outcome<O>): Outcome<O>
}

/** Composes [Policy] instances into a single pipeline. See [Policy] for why this mirrors kiit.policy. */
object Policies {
    /**
     * Chains [all] into one pipeline ending in [last]. Right-folded, so the first entry in [all]
     * is outermost. It runs first and decides whether the rest of the chain, down to [last],
     * gets called at all.
     */
    fun <I, O> chain(all: List<Policy<I, O>>, last: suspend (I) -> Outcome<O>): suspend (I) -> Outcome<O> =
        all.foldRight(last) { policy, next -> compose(policy, next) }

    fun <I, O> compose(p: Policy<I, O>, op: suspend (I) -> Outcome<O>): suspend (I) -> Outcome<O> {
        return { i -> p.run(i, op) }
    }
}

/** [Policy] specialized for request/response interception (logging, retry, diagnostics, etc.). */
typealias RpcPolicy = Policy<RpcRequest, RpcResponse>
