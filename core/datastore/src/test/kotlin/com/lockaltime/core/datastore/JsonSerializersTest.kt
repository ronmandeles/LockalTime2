package com.lockaltime.core.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class JsonSerializersTest {

    private val session = SessionEntity(
        id = "s1",
        name = "Study",
        blockedApps = listOf(BlockedAppEntity("a.pkg", "A")),
        createdAtMillis = 0,
    )
    private val activeSession = ActiveSessionEntity(
        sessionId = "s1",
        name = "Study",
        blockedPackages = setOf("a.pkg", "b.pkg"),
        startedAtMillis = 1_000,
        endsAtMillis = 2_000,
    )

    // The exact files written by the first app version, before storage models were split out.
    @Test
    fun `reads sessions stored by earlier app versions`() = runTest {
        val json = """{"sessions":[{"id":"s1","name":"Study","blockedApps":[{"packageName":"a.pkg","label":"A"}],"createdAtMillis":0}]}"""

        assertEquals(SessionsData(listOf(session)), SessionsDataSerializer.readFrom(json.byteInputStream()))
    }

    @Test
    fun `reads an active session stored by earlier app versions`() = runTest {
        val json = """{"session":{"sessionId":"s1","name":"Study","blockedPackages":["a.pkg","b.pkg"],"startedAtMillis":1000,"endsAtMillis":2000}}"""

        assertEquals(ActiveSessionData(activeSession), ActiveSessionDataSerializer.readFrom(json.byteInputStream()))
    }

    @Test
    fun `ignores fields it doesn't know`() = runTest {
        val json = """{"session":null,"addedLater":true}"""

        assertEquals(ActiveSessionData(), ActiveSessionDataSerializer.readFrom(json.byteInputStream()))
    }

    @Test
    fun `round-trips data`() = runTest {
        val data = SessionsData(listOf(session))
        val output = ByteArrayOutputStream()

        SessionsDataSerializer.writeTo(data, output)

        assertEquals(data, SessionsDataSerializer.readFrom(ByteArrayInputStream(output.toByteArray())))
    }

    @Test(expected = CorruptionException::class)
    fun `reports unreadable data as corruption`() = runTest {
        SessionsDataSerializer.readFrom("not json".byteInputStream())
    }
}
