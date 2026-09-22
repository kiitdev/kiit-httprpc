package kiit.rpc

import kiit.result.Outcome

/**
 * Mirrors `kiit.policy`'s existing `Policy<I, O>` shape (`kiit/src/internal/policy`, currently
 * unpublished/JVM-only in the monorepo) rather than inventing new middleware terminology — same
 * mental model, same composition semantics, reimplemented locally here since that module isn't
 * published anywhere yet. Swappable for a real dependency later if `kiit-policy` is ever
 * extracted into its own published KMP repo.
 *
 * A [Policy] wraps [operation]: it decides whether/how to call it (retry, log around it, rewrite
 * [i] first, short-circuit without calling it at all) and must produce an [Outcome].
 *
 * @param I : Input type
 * @param O : Output type
 */
interface Policy<I, O> {
    suspend fun run(i: I, operation: suspend (I) -> Outcome<O>): Outcome<O>
}

/** Composes [Policy] instances into a single pipeline. See [Policy] for why this mirrors kiit.policy. */
object Policies {
    /**
     * Chains [all] into one pipeline ending in [last]. Right-folded, so the *first* entry in
     * [all] is outermost — it runs first and decides whether/how the rest of the chain (down to
     * [last]) gets called at all.
     */
    fun <I, O> chain(all: List<Policy<I, O>>, last: suspend (I) -> Outcome<O>): suspend (I) -> Outcome<O> =
        all.foldRight(last) { policy, next -> compose(policy, next) }

    fun <I, O> compose(p: Policy<I, O>, op: suspend (I) -> Outcome<O>): suspend (I) -> Outcome<O> {
        return { i -> p.run(i, op) }
    }
}

/** [Policy] specialized for HTTP request/response interception (logging, retry, diagnostics, etc.). */
typealias HttpRpcPolicy = Policy<HttpRpcRequest, HttpRpcResponse>
