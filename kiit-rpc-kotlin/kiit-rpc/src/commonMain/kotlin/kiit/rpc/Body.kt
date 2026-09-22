package kiit.rpc

/**
 * Request body variants. All are pre-serialized/raw. [Serializer] is the separate, higher-level
 * path for typed values, used by `executeResult`/`executeOutcome`. There's no `JsonObject`-style
 * variant here bridging the two.
 */
sealed class Body {
    data class FormData(val values: List<Pair<String, String>>) : Body()

    data class MultiPart(val values: List<Pair<String, Content>>) : Body()

    data class RawContent(val content: String) : Body()

    data class JsonContent(val content: String) : Body()
}
