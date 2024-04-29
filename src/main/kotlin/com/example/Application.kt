package com.example

import com.example.plugins.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}


fun Application.module() {
    configureRouting()
    configureSerialization()
}
