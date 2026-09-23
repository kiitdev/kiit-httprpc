package kiit.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Encode/decode abstraction backing `executeResult`/`executeOutcome`. kotlinx.serialization is
 * only the default implementation ([KotlinxSerializer], via the [Serializer] factory below). A
 * consumer who wants Moshi/Gson/Jackson on JVM implements this directly instead, so kiit-rpc's
 * core API isn't hard-wired to one JSON library.
 */
interface Serializer<T> {
    fun encode(value: T): String

    fun decode(content: String): T
}

/**
 * Default JSON config for [KotlinxSerializer]/[Serializer]. `ignoreUnknownKeys = true`: the whole
 * point of a typed decode is picking out the fields you care about, a real API response almost
 * always has more fields than any one consumer's type declares. kotlinx.serialization's own
 * default is strict about this and throws instead.
 */
val DefaultJson: Json = Json { ignoreUnknownKeys = true }

class KotlinxSerializer<T>(
    private val serializer: KSerializer<T>,
    private val json: Json = DefaultJson,
) : Serializer<T> {
    override fun encode(value: T): String = json.encodeToString(serializer, value)

    override fun decode(content: String): T = json.decodeFromString(serializer, content)
}

/**
 * Reified convenience factory for the common `@Serializable` case. Kotlin-only: `reified` inline
 * functions never exist in the compiled framework, so Swift can't call this. Swift callers build
 * a [KotlinxSerializer] directly with an explicit `T.serializer()`.
 */
@Suppress("ktlint:standard:function-naming")
inline fun <reified T> Serializer(json: Json = DefaultJson): Serializer<T> = KotlinxSerializer(serializer(), json)
