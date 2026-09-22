package kiit.rpc

import kiit.codes.Codes
import kiit.codes.Excluded
import kiit.codes.Information
import kiit.codes.Invalid
import kiit.codes.Pending
import kiit.codes.Rejected
import kiit.codes.Restricted
import kiit.codes.Status
import kiit.codes.StatusConstants
import kiit.codes.Succeeded
import kiit.codes.Unserved
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Parses [body] as a JSON object, or null if it isn't valid JSON or isn't an object — both are
 * expected, non-exceptional outcomes here (most response bodies match neither structured shape
 * below), so the caught exceptions are deliberately not rethrown/logged.
 */
@Suppress("SwallowedException")
private fun parseJsonObjectOrNull(body: String): JsonObject? =
    try {
        Json.parseToJsonElement(body).jsonObject
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalStateException) {
        null
    }

private fun JsonObject.stringField(key: String): String? {
    return (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}

/**
 * Reconstructs the [Status] a `code` string (`"Failed:Rejected:DUPLICATE_CHARGE"`, from either
 * `CodeDetail.code` or a kiit-origin `Problem.type`) describes: a kiit built-in when [origin]
 * matches the registry, else a direct subtype construction from [group]/[name].
 */
private fun statusFromCode(code: String, origin: String, scope: String, message: String): Status? {
    val parts = code.split(":")
    if (parts.size != 3) return null
    val group = parts[1]
    val name = parts[2]
    return Codes.statusFor(origin, group, name) ?: when (group) {
        "Succeeded" -> Succeeded(name, message, origin, scope)
        "Pending" -> Pending(name, message, origin, scope)
        "Excluded" -> Excluded(name, message, origin, scope)
        "Information" -> Information(name, message, origin, scope)
        "Restricted" -> Restricted(name, message, origin, scope)
        "Invalid" -> Invalid(name, message, origin, scope)
        "Rejected" -> Rejected(name, message, origin, scope)
        "Unserved" -> Unserved(name, message, origin, scope)
        else -> null
    }
}

/**
 * `CodeDetail` shape (`kiit.codes.formats.CodeDetail`'s `path`/`code`/`success`/`message`) —
 * deliberately a loose field-shape check, not a strict `@Serializable` decode: an unrelated
 * third-party body that happens to have an unrelated `code` field should fail this harmlessly.
 */
private fun JsonObject.codeDetailStatusOrNull(): Status? {
    val code = stringField("code") ?: return null
    val path = stringField("path") ?: return null
    val message = stringField("message") ?: return null
    (this["success"] as? JsonPrimitive)?.booleanOrNull ?: return null

    val originAndScope = path.split(":", limit = 2)
    val origin = originAndScope[0]
    val scope = originAndScope.getOrElse(1) { "" }
    return statusFromCode(code, origin = origin, scope = scope, message = message)
}

/**
 * RFC 9457 `Problem` shape (`type`/`title`/`status`). Only the kiit-origin special case embeds a
 * `code` — `?code=Failed:Rejected:DUPLICATE_CHARGE#taxonomy` appended to kiit's own docs base URL,
 * see `kiit.codes.formats.defaultTypeBuilder` — so only that case is reconstructed; a custom
 * origin's dash-path `type` (`"payments.cards/rejected/duplicate-charge"`) doesn't carry enough
 * back (the origin itself depends on a `baseUrls` mapping this client has no way to know), so it
 * isn't attempted.
 */
private fun JsonObject.problemStatusOrNull(): Status? {
    val type = stringField("type") ?: return null
    val title = stringField("title") ?: return null
    val code = type.substringAfter("?code=", "").substringBefore("#")
    if (code.isEmpty()) return null
    return statusFromCode(code, origin = StatusConstants.KIIT, scope = "", message = title)
}

/**
 * Reconstructs a [Status] from a response body carrying either kiit-native structured error
 * shape (`CodeDetail` or a kiit-origin `Problem`). Returns null when [body] matches neither —
 * [HttpRpcResponse.resolveStatus] falls back to [toStatus] (the HTTP status code table) then.
 */
fun structuredStatusOrNull(body: String): Status? {
    val obj = parseJsonObjectOrNull(body) ?: return null
    return obj.codeDetailStatusOrNull() ?: obj.problemStatusOrNull()
}

/**
 * Resolves the [Status] for this response: a structured kiit body ([structuredStatusOrNull])
 * takes precedence when present, falling back to the plain HTTP status code ([Int.toStatus])
 * otherwise.
 */
fun HttpRpcResponse.resolveStatus(): Status {
    val looksJson =
        headers.entries.any { (key, value) ->
            key.equals("Content-Type", ignoreCase = true) && value.contains("json", ignoreCase = true)
        }
    val fromBody = if (looksJson && body.isNotBlank()) structuredStatusOrNull(body) else null
    return fromBody ?: status.toStatus()
}
