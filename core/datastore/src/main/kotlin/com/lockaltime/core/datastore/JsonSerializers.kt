package com.lockaltime.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object SessionsDataSerializer :
    JsonSerializer<SessionsData>(SessionsData.serializer(), SessionsData())

object ActiveSessionDataSerializer :
    JsonSerializer<ActiveSessionData>(ActiveSessionData.serializer(), ActiveSessionData())

// Unknown keys are ignored so older app versions can read files written by newer ones.
private val storageJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

sealed class JsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T = try {
        storageJson.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unable to read stored data", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(storageJson.encodeToString(serializer, t).encodeToByteArray())
    }
}
