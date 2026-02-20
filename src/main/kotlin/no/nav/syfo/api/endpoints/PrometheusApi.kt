package no.nav.syfo.api.endpoints

import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import no.nav.syfo.infrastructure.METRICS_REGISTRY

fun Routing.registerPrometheusApi() {
    get("/metrics") {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
