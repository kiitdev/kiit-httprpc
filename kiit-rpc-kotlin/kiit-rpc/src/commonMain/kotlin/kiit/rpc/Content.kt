package kiit.rpc

/**
 * Minimal local replacement for `kiit.common.types.{Content,ContentFile,ContentText}` — just
 * enough for [Body.MultiPart], not a full port of kiit-common (kiit-httprpc has no dependency on
 * kiit-common at all).
 */
sealed class Content {
    abstract val name: String
}

data class ContentText(val text: String, override val name: String = "") : Content()

data class ContentFile(
    override val name: String,
    val type: ContentType,
    val data: ByteArray,
) : Content() {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is ContentFile &&
                    name == other.name &&
                    type == other.type &&
                    data.contentEquals(other.data)
            )

    override fun hashCode(): Int = name.hashCode() * 31 + type.hashCode() * 31 + data.contentHashCode()
}

/** Just enough of a MIME-type carrier for multipart parts — open, not a closed enum, so a caller
 * can construct any MIME type without waiting on this list to grow. */
data class ContentType(val http: String) {
    companion object {
        val Plain = ContentType("text/plain")
        val Json = ContentType("application/json")
        val OctetStream = ContentType("application/octet-stream")
    }
}
