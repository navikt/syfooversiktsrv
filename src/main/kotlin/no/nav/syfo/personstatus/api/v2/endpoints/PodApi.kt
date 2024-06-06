package no.nav.syfo.personstatus.api.v2.endpoints

import io.ktor.server.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import kotlin.use

const val podLivenessPath = "/internal/is_alive"
const val podReadinessPath = "/internal/is_ready"

fun Routing.registerPodApi(
    applicationState: ApplicationState,
    database: DatabaseInterface,
) {
    get(podLivenessPath) {
        if (applicationState.alive) {
            call.respondText(
                text = "I'm alive! :)",
            )
        } else {
            call.respondText(
                text = "I'm dead x_x",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
    get(podReadinessPath) {
        val isReady = isReady(
            applicationState = applicationState,
            database = database,
        )
        if (isReady) {
            call.respondText(
                text = "I'm ready! :)",
            )
        } else {
            call.respondText(
                text = "Please wait! I'm not ready :(",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}

private fun isReady(
    applicationState: ApplicationState,
    database: DatabaseInterface,
): Boolean {
    return applicationState.ready && database.isOk()
}

private fun DatabaseInterface.isOk(): Boolean {
    return try {
        connection.use {
            it.isValid(1)
        }
    } catch (ex: Exception) {
        false
    }
}
