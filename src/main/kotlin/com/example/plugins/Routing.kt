package com.example.plugins

import com.example.models.User
import com.example.models.checkPassword
import com.example.models.hashPassword
import com.example.models.userStorage
import com.example.models.validateFingerprintData
import io.ktor.server.application.*
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("/user") {
            post("/login") {
                val formParameters = call.receiveParameters()
                val email = formParameters["email"].toString()
                val password = formParameters["password"].toString()
                val visitorId = formParameters["visitorId"].toString()
                val requestId = formParameters["requestId"].toString()
                println("----$visitorId")
                val user =
                    userStorage.firstOrNull {
                        it.email == email && checkPassword(
                            password,
                            it.password
                        )
                    }
                if (user != null) {
                    println(user)
                    if (user.visitorId == visitorId) {
                        call.respond(user)
                    } else {
                        // If the fingerprint in the login request doesn't match their user profile, it
                        // indicates that they are using a different device.
                        // This sample API returns an error for this scenario. Still, in a real-world
                        // application, you could ask for a 2FA authentication such as OTP, TOTP, etc.
                        call.respond(JsonPrimitive("NEW DIGITAL FINGERPRINT DETECTED!!!"))
                    }
                } else {
                    call.respond(JsonPrimitive("USER NOT FOUND!!!"))
                }
            }
            post("/sign-up") {
                val formParameters = call.receiveParameters()
                val email = formParameters["email"].toString()
                val password = formParameters["password"].toString()
                val name = formParameters["name"].toString()
                val visitorId = formParameters["visitorId"].toString()
                val requestId = formParameters["requestId"].toString()
                println("----$visitorId")
                val exists =
                    userStorage.firstOrNull() { t -> t.visitorId == visitorId || t.email == email }
                if (exists == null) {
                    val validations = async { validateFingerprintData(requestId, visitorId) }
                    val validationResult = validations.await()

                    if (validationResult.success) {
                        val user = User(
                            name = name,
                            email = email,
                            password = hashPassword(password),
                            visitorId = visitorId,
                            requestId = requestId
                        )
                        userStorage.add(user)
                        println(user)
                        call.respond(user)
                    } else {
                        call.respond(JsonPrimitive(validationResult.errorMessage))
                    }
                } else {
                    call.respond(JsonPrimitive("USER WITH THE SAME EMAIL OR visitorId EXISTS!!!"))
                }

            }
        }
    }
}
