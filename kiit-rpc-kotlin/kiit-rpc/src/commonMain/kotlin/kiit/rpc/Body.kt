package kiit.rpc

/**
 * Request body variants. All are pre-serialized/raw — [kiit.httprpc.Serializer] (typed encode/decode
 * for `executeResult`/`executeOutcome`) is the separate, higher-level path for typed values;
 * there's no `JsonObject`-style variant here that tries to bridge the two, that's what the
 * original source got wrong.
 */
sealed class Body {
    data class FormData(val values: List<Pair<String, String>>) : Body()

    data class MultiPart(val values: List<Pair<String, Content>>) : Body()

    data class RawContent(val content: String) : Body()

    data class JsonContent(val content: String) : Body()
}
