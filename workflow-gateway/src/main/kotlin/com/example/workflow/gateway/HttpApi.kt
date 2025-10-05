package com.example.workflow.gateway

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

/**
 * Minimal HTTP gateway forwarding workflow commands to Kafka.
 */
fun Application.workflowGateway(send: suspend (String, String, String) -> Unit) {
    routing {
        post("/workflows/{type}/{id}/commands/{command}") {
            val type = call.parameters.getValue("type")
            val id = call.parameters.getValue("id")
            val command = call.parameters.getValue("command")
            val payload = call.receiveText()
            send(type, id, payload)
            call.respondText(status = HttpStatusCode.Accepted, text = "enqueued")
        }
    }
}
