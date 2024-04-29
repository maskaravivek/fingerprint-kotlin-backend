package com.example.models

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import java.util.Date
import kotlin.Exception

@Serializable
data class User(
    val name: String,
    val password: String,
    val email: String,
    val visitorId: String?,
    val requestId: String?
)

@Serializable
data class EventResponse(val products: Products)

@Serializable
data class Products(val identification: Identification)

@Serializable
data class Identification(val data: IdentificationData)

@Serializable
data class IdentificationData(
    val visitorId: String,
    val timestamp: Long,
    val confidence: Confidence
)

@Serializable
data class Confidence(val score: Int)

data class ValidatationResult(val success: Boolean, val errorMessage: String? = "")

val maxRequestLifespan = 5 * 60 * 1000;
val minConfidence = 0.5
val userStorage = mutableListOf<User>()

fun hashPassword(plaintextPassword: String): String {
    val salt = BCrypt.gensalt()
    return BCrypt.hashpw(plaintextPassword, salt)
}

fun checkPassword(plaintextPassword: String, hashedPassword: String): Boolean {
    return BCrypt.checkpw(plaintextPassword, hashedPassword)
}

suspend fun validateFingerprintData(requestId: String, visitorId: String?): ValidatationResult {
    val client = HttpClient()

    try {
        val response = client.get("https://api.fpjs.io/events/$requestId") {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header("Auth-API-Key", "your-secret-api-key")
        }
        val stringBody: String = response.body()

        client.close()

        val respJson = Json {
            ignoreUnknownKeys = true
        }.decodeFromString<EventResponse>(stringBody)
        val identification = respJson.products.identification.data

        val serverVisitorId = identification.visitorId
        val identificationTimestamp = identification.timestamp
        val confidence = identification.confidence.score

        val timeNow = Date().time

        if (timeNow - identificationTimestamp > maxRequestLifespan) {
            return ValidatationResult(false, "Fingerprint request expired.")
        }

        if (!serverVisitorId.equals(visitorId)) {
            return ValidatationResult(false, "Fingerprint forgery detected.")
        }

        if (confidence < minConfidence) {
            return ValidatationResult(false, "Fingerprint confidence too low.")
        }
    } catch (exception: Exception) {
        println(exception.message)
        return ValidatationResult(false, "Invalid fingerprint.")
    }

    return ValidatationResult(true);
}