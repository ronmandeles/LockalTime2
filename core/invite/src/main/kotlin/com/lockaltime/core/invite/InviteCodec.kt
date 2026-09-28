package com.lockaltime.core.invite

import com.lockaltime.core.model.SessionInvite
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

sealed interface DecodedInvite {
    data class Valid(val invite: SessionInvite) : DecodedInvite

    /** Made by a newer app version, in a format this one can't read. */
    data object TooNew : DecodedInvite

    /** Anything else: a different QR code, a damaged one, or one with invalid content. */
    data object NotAnInvite : DecodedInvite
}

/**
 * Turns a [SessionInvite] into a short string for a QR code and back.
 *
 * Format: `lockaltime://join?d=` + base64url(zlib(JSON of [InviteWire])). It's a URI so the app
 * can claim it as a deep link and the system camera can open it directly. Compression roughly
 * halves the size, since package names share prefixes like `com.`.
 *
 * This runs on Android (minSdk 26) as well as the JVM, so it sticks to Java APIs available there.
 */
object InviteCodec {

    /** Longer codes make QR codes too dense to scan reliably from a phone screen. */
    const val MAX_CODE_LENGTH = 1_500

    /** What every code starts with. The deep link that opens the app is built from it. */
    const val URI_PREFIX = "lockaltime://join?d="

    private const val VERSION = 1
    private const val MAX_PACKAGES = 100

    // Caps decompression so a crafted code can't expand into a huge payload.
    private const val MAX_JSON_BYTES = 16 * 1024

    // Java package names as Android requires them: at least two segments.
    private val packageName = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    // Unknown keys are ignored so this version can read codes with fields added later.
    private val inviteJson = Json { ignoreUnknownKeys = true }

    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()
    private val base64Decoder = Base64.getUrlDecoder()

    /** @return null if the session is too big to fit in a scannable code. */
    fun encode(invite: SessionInvite): String? {
        if (!isValid(invite)) return null
        val json = inviteJson.encodeToString(InviteWire.serializer(), invite.asWire(VERSION))
        val code = URI_PREFIX + base64Encoder.encodeToString(deflate(json.encodeToByteArray()))
        return code.takeIf { it.length <= MAX_CODE_LENGTH }
    }

    fun decode(code: String): DecodedInvite {
        val data = code.trim()
        if (!data.startsWith(URI_PREFIX)) return DecodedInvite.NotAnInvite
        return try {
            val json = inflate(base64Decoder.decode(data.removePrefix(URI_PREFIX)))
                ?: return DecodedInvite.NotAnInvite
            val element = inviteJson.parseToJsonElement(json.decodeToString())
            // Read the version first: a newer format may not fit InviteWire at all.
            val version = element.jsonObject["v"]?.jsonPrimitive?.intOrNull
                ?: return DecodedInvite.NotAnInvite
            if (version > VERSION) return DecodedInvite.TooNew
            val invite = inviteJson.decodeFromJsonElement(InviteWire.serializer(), element)
                .asExternalModel()
            if (isValid(invite)) DecodedInvite.Valid(invite) else DecodedInvite.NotAnInvite
        } catch (e: IllegalArgumentException) {
            // Bad base64, bad JSON (SerializationException is a subclass), or JSON that isn't an object.
            DecodedInvite.NotAnInvite
        } catch (e: DataFormatException) {
            DecodedInvite.NotAnInvite
        }
    }

    private fun isValid(invite: SessionInvite): Boolean =
        invite.sessionId.isNotBlank() &&
            invite.name.isNotBlank() &&
            invite.blockedPackages.size in 1..MAX_PACKAGES &&
            invite.blockedPackages.all { packageName.matches(it) }

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        try {
            deflater.setInput(bytes)
            deflater.finish()
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (!deflater.finished()) {
                output.write(buffer, 0, deflater.deflate(buffer))
            }
            return output.toByteArray()
        } finally {
            deflater.end()
        }
    }

    /** @return null if the data is truncated or inflates past [MAX_JSON_BYTES]. */
    private fun inflate(bytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        try {
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                output.write(buffer, 0, count)
                if (output.size() > MAX_JSON_BYTES) return null
            }
            return output.toByteArray()
        } finally {
            inflater.end()
        }
    }

    private const val BUFFER_SIZE = 1024
}
