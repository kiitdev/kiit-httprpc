package kiit.rpc.http

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
import kiit.inputs.Meta
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject

private const val HEADER_STATUS = "x-server-status-rfc9457"
private const val HEADER_TITLE = "x-server-status-rfc9457-title"

/**
 * Parses [body] as a JSON object, or null if it isn't valid JSON or isn't an object. Both are
 * expected outcomes here, most response bodies match neither structured shape below, so the
 * caught exceptions are deliberately not rethrown or logged.
 */
@Suppress("SwallowedException")
private fun parseJsonObjectOrNull(body: String): JsonObject? =
    try {
        Json.parseToJsonElement(body).jsonObject
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        // .jsonObject throws this when the parsed element isn't actually a JsonObject.
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
private fun statusFromCode(
    code: String,
    origin: String,
    scope: String,
    message: String
): Status? {
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
 * `CodeDetail` shape (`kiit.codes.formats.CodeDetail`'s `path`/`code`/`success`/`message`). A
 * loose field-shape check, not a strict `@Serializable` decode. An unrelated third-party body
 * with its own unrelated `code` field should fail this harmlessly.
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
 * RFC 9457 `Problem` shape (`type`/`title`/`status`).
 *
 * 1. Only the kiit-origin case embeds a `code`: `?code=Failed:Rejected:DUPLICATE_CHARGE#taxonomy`
 *    appended to kiit's docs base URL (`kiit.codes.formats.defaultTypeBuilder`).
 * 2. A custom origin's dash-path `type` (`"payments.cards/rejected/duplicate-charge"`) isn't
 *    reconstructed. The origin itself depends on a `baseUrls` mapping this client can't know.
 */
private fun JsonObject.problemStatusOrNull(): Status? {
    val type = stringField("type") ?: return null
    val title = stringField("title") ?: return null
    val code = type.substringAfter("?code=", "").substringBefore("#")
    if (code.isEmpty()) return null
    return statusFromCode(code, origin = StatusConstants.KIIT, scope = "", message = title)
}

/**
 * Reconstructs a [Status] from a response body carrying a kiit-native structured error shape,
 * either `CodeDetail` or a kiit-origin `Problem`. Returns null when [body] matches neither, and
 * [KiitStatusConverter] falls back to [Int.toStatus] then.
 *
 * Internal: a caller wanting this behavior goes through [StatusConverter]/[KiitStatusConverter].
 */
internal fun structuredStatusOrNull(body: String): Status? {
    val obj = parseJsonObjectOrNull(body) ?: return null
    return obj.codeDetailStatusOrNull() ?: obj.problemStatusOrNull()
}

private fun String.kebabToPascalCase(): String = replaceFirstChar { it.uppercase() }

private fun String.kebabToUpperSnakeCase(): String = replace("-", "_").uppercase()

/**
 * Reconstructs a [Status] from the `x-server-status-rfc9457`/`-title` response headers, if
 * present. Value is an RFC 9457-shaped URI encoding origin/scope/group/code by position, e.g.
 * `https://stripe.com/problems/payments.cards/rejected/duplicate-charge` gives origin=`stripe.com`,
 * scope=`payments.cards`, group=`rejected`, code=`duplicate-charge`. The second segment
 * (`problems`) is a fixed prefix, ignored. `group`/`code` come in kebab-case and get normalized
 * to kiit-codes' own PascalCase/UPPER_SNAKE_CASE before the registry lookup.
 *
 * Unlike [problemStatusOrNull], this handles a custom origin directly, no `baseUrls` mapping
 * needed since origin/scope are already in the header. No body access either, so
 * [KiitStatusConverter] checks this before any body parsing.
 */
internal fun headerStatusOrNull(meta: Meta): Status? {
    val header = meta.getStringOrNull(HEADER_STATUS) ?: return null
    val title = meta.getStringOrNull(HEADER_TITLE) ?: ""
    val segments = header.substringAfter("://", header).split("/").filter { it.isNotEmpty() }
    if (segments.size < 5) return null
    val origin = segments[0]
    // segments[1] is the fixed namespace prefix, intentionally ignored.
    val scope = segments[2]
    val group = segments[3].kebabToPascalCase()
    val code = segments[4].kebabToUpperSnakeCase()
    return statusFromCode("Failed:$group:$code", origin = origin, scope = scope, message = title)
}
