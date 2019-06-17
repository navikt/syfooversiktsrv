package no.nav.syfo.tilgangskontroll

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory

val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.tilgangskontroll")

class TilgangskontrollConsumer(
        private val endpointUrl: String,
        private val client: HttpClient
) {
    private val paramFnr = "fnr"
    private val pathTilgangTilBruker = "/bruker"

    suspend fun harVeilederTilgangTilPerson(fnr: String, token: String): Boolean {
        log.info("JTRACE: sjekkertilgang til {} for {} mot url {}", fnr, token, "$endpointUrl/syfo-tilgangskontroll/api/tilgang$pathTilgangTilBruker")

        val response = client.get<Tilgang>("$endpointUrl/syfo-tilgangskontroll/api/tilgang$pathTilgangTilBruker") {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $token")
            }
            parameter(paramFnr, fnr)
        }
        return response.harTilgang
    }
}
