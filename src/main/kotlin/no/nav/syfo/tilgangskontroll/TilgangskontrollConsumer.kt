package no.nav.syfo.tilgangskontroll

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import no.nav.syfo.metric.*
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("no.nav.syfo.oversikt.tilgangskontroll")

class TilgangskontrollConsumer(
        private val endpointUrl: String
) {
    private val paramEnhet = "enhet"
    private val pathTilgangTilBrukere = "/brukere"
    private val pathTilgangTilEnhet = "/enhet"

    fun veilederPersonAccessList(fnrList: List<String>, token: String, callId: String): List<String>? {
        val bodyJson = objectMapper.writeValueAsString(fnrList)

        val requestTimer = HISTOGRAM_SYFOTILGANGSKONTROLL_PERSONER.startTimer()

        val (_, _, result) = getTilgangskontrollUrl(pathTilgangTilBrukere).httpPost()
                .body(bodyJson)
                .header(mapOf(
                        "Authorization" to bearerHeader(token),
                        "Content-Type" to "application/json",
                        NAV_CALL_ID_HEADER to callId
                ))
                .responseString()

        requestTimer.observeDuration()

        result.fold(success = {
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS.inc()
            return objectMapper.readValue<List<String>>(result.get())
        }, failure = {
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.inc()
            val exception = it.exception
            log.error("Error while requesting access to list of person from syfo-tilgangskontroll: ${exception.message}", exception)
            return null
        })
    }

    fun harVeilederTilgangTilEnhet(enhet: String, token: String, callId: String): Boolean {
        val requestTimer = HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET.startTimer()
        val (_, response, _) = getTilgangskontrollUrl("$pathTilgangTilEnhet?${paramEnhet}=$enhet").httpGet()
                .header(mapOf(
                        "Authorization" to bearerHeader(token),
                        "Accept" to "application/json",
                        NAV_CALL_ID_HEADER to callId
                ))
                .responseString()
        requestTimer.observeDuration()

        return response.statusCode in 200..2999
    }

    private fun getTilgangskontrollUrl(path: String): String {
        return "$endpointUrl/syfo-tilgangskontroll/api/tilgang$path"
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
