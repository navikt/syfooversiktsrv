package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.metric.METRICS_REGISTRY

fun Routing.registerPrometheusApi() {
    get("/metrics") {
        call.respondText(METRICS_REGISTRY.scrape())
    }
}
