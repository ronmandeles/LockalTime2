package com.lockaltime.core.invite

import com.lockaltime.core.model.SessionInvite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.util.zip.Deflater
import kotlin.random.Random

class InviteCodecTest {

    private val timed = SessionInvite(
        sessionId = "s1",
        name = "Study",
        blockedPackages = setOf("com.instagram.android", "com.zhiliaoapp.musically"),
        endsAtMillis = 1_727_430_000_000,
    )

    @Test
    fun `round-trips a timed invite`() {
        assertEquals(DecodedInvite.Valid(timed), InviteCodec.decode(InviteCodec.encode(timed)!!))
    }

    @Test
    fun `round-trips an open-ended invite`() {
        val openEnded = timed.copy(endsAtMillis = null)

        assertEquals(DecodedInvite.Valid(openEnded), InviteCodec.decode(InviteCodec.encode(openEnded)!!))
    }

    @Test
    fun `codes are URIs`() {
        // The literal is repeated in the app manifest's intent-filter; keep them in step.
        assertEquals("lockaltime://join?d=", InviteCodec.URI_PREFIX)
        assertTrue(InviteCodec.encode(timed)!!.startsWith(InviteCodec.URI_PREFIX))
    }

    // The v1 format: codes shown by this version must stay readable by later ones.
    @Test
    fun `reads the v1 format`() {
        val json = """{"v":1,"id":"s1","n":"Study","p":["com.instagram.android","com.zhiliaoapp.musically"],"e":1727430000000}"""

        assertEquals(DecodedInvite.Valid(timed), InviteCodec.decode(codeFor(json)))
    }

    @Test
    fun `ignores fields it doesn't know`() {
        val json = """{"v":1,"id":"s1","n":"Study","p":["a.pkg"],"addedLater":true}"""

        assertEquals(
            DecodedInvite.Valid(SessionInvite("s1", "Study", setOf("a.pkg"))),
            InviteCodec.decode(codeFor(json)),
        )
    }

    @Test
    fun `reports codes from a newer format`() {
        val json = """{"v":2,"somethingElse":[1,2,3]}"""

        assertEquals(DecodedInvite.TooNew, InviteCodec.decode(codeFor(json)))
    }

    @Test
    fun `tolerates surrounding whitespace`() {
        assertEquals(DecodedInvite.Valid(timed), InviteCodec.decode("  ${InviteCodec.encode(timed)}\n"))
    }

    @Test
    fun `rejects codes that aren't invites`() {
        listOf(
            "",
            "https://example.com",
            "lockaltime://join?d=",
            "lockaltime://join?d=not*base64",
            "lockaltime://join?d=" + Base64.getUrlEncoder().encodeToString("not zlib".toByteArray()),
            codeFor("not json"),
            codeFor("[1,2]"),
            codeFor("""{"id":"s1","n":"Study","p":["a.pkg"]}"""),
            codeFor("""{"v":1,"id":"s1","n":"Study"}"""),
            InviteCodec.encode(timed)!!.dropLast(10),
        ).forEach { code ->
            assertEquals(code, DecodedInvite.NotAnInvite, InviteCodec.decode(code))
        }
    }

    @Test
    fun `rejects invites with invalid content`() {
        listOf(
            """{"v":1,"id":"","n":"Study","p":["a.pkg"]}""",
            """{"v":1,"id":"s1","n":" ","p":["a.pkg"]}""",
            """{"v":1,"id":"s1","n":"Study","p":[]}""",
            """{"v":1,"id":"s1","n":"Study","p":["nodots"]}""",
            """{"v":1,"id":"s1","n":"Study","p":["a.pkg; rm -rf"]}""",
        ).forEach { json ->
            assertEquals(json, DecodedInvite.NotAnInvite, InviteCodec.decode(codeFor(json)))
        }
    }

    @Test
    fun `rejects payloads that inflate too far`() {
        val json = """{"v":1,"id":"s1","n":"${"x".repeat(20_000)}","p":["a.pkg"]}"""

        assertEquals(DecodedInvite.NotAnInvite, InviteCodec.decode(codeFor(json)))
    }

    @Test
    fun `doesn't encode sessions too big for a QR code`() {
        val huge = timed.copy(blockedPackages = packageNames(count = 100, segmentLength = 10))

        assertNull(InviteCodec.encode(huge))
    }

    @Test
    fun `encodes a typical session well under the size limit`() {
        val typical = timed.copy(blockedPackages = packageNames(count = 30, segmentLength = 8))

        assertTrue(InviteCodec.encode(typical)!!.length < InviteCodec.MAX_CODE_LENGTH / 2)
    }

    // Varied names: repetitive ones like "com.example.app1" compress far better than real ones.
    private fun packageNames(count: Int, segmentLength: Int): Set<String> {
        val random = Random(seed = 42)
        fun segment() = String(CharArray(segmentLength) { 'a' + random.nextInt(26) })
        return (1..count).mapTo(mutableSetOf()) { "com.${segment()}.${segment()}" }
    }

    private fun codeFor(json: String): String {
        val deflater = Deflater()
        deflater.setInput(json.toByteArray())
        deflater.finish()
        val buffer = ByteArray(64 * 1024)
        val size = deflater.deflate(buffer)
        deflater.end()
        return "lockaltime://join?d=" +
            Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.copyOf(size))
    }
}
