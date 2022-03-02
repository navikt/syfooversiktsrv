package no.nav.syfo.application.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface

const val podLivenessPath = "/is_alive"
const val podReadinessPath = "/is_ready"

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
