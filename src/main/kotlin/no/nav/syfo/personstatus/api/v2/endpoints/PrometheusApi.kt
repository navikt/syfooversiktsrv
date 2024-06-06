package no.nav.syfo.personstatus.api.v2.endpoints

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import no.nav.syfo.metric.METRICS_REGISTRY

fun Routing.registerPrometheusApi() {
    get("/metrics") {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
