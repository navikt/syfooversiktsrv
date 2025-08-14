package no.nav.syfo.personstatus.infrastructure.clients.ereg

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.cache.ValkeyStore
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import org.slf4j.LoggerFactory

class EregClient(
    clientEnvironment: ClientEnvironment,
    private val valkeyStore: ValkeyStore,
    private val httpClient: HttpClient = httpClientDefault(),
) {

    private val eregOrganisasjonUrl: String = "${clientEnvironment.baseUrl}/$EREG_PATH"

    suspend fun organisasjonVirksomhetsnavn(
        callId: String,
        virksomhetsnummer: Virksomhetsnummer,
    ): EregVirksomhetsnavn? {
        val cacheKey = "$CACHE_EREG_VIRKSOMHETSNAVN_KEY_PREFIX${virksomhetsnummer.value}"
        val cachedResponse: EregVirksomhetsnavn? = valkeyStore.getObject(key = cacheKey)

        if (cachedResponse != null) {
            return cachedResponse
        } else {
            try {
                val url = "$eregOrganisasjonUrl/${virksomhetsnummer.value}"
                val response = httpClient.get(url) {
                    header(NAV_CALL_ID_HEADER, callId)
                    accept(ContentType.Application.Json)
                }.body<EregOrganisasjonResponse>()
                COUNT_CALL_EREG_ORGANISASJON_SUCCESS.increment()
                val eregVirksomhetsnavn = response.toEregVirksomhetsnavn()
                valkeyStore.setObject(
                    key = cacheKey,
                    value = eregVirksomhetsnavn,
                    expireSeconds = CACHE_EREG_VIRKSOMHETSNAVN_TIME_TO_LIVE_SECONDS,
                )
                return eregVirksomhetsnavn
            } catch (e: ResponseException) {
                if (e.isOrganisasjonNotFound(virksomhetsnummer)) {
                    log.warn("No Organisasjon was found in Ereg: returning empty Virksomhetsnavn, message=${e.message}, callId=$callId")
                    COUNT_CALL_EREG_ORGANISASJON_NOT_FOUND.increment()
                    return EregVirksomhetsnavn(
                        virksomhetsnavn = "",
                    )
                } else {
                    log.error(
                        "Error while requesting Response from Ereg {}, {}, {}",
                        StructuredArguments.keyValue("statusCode", e.response.status.value.toString()),
                        StructuredArguments.keyValue("message", e.message),
                        StructuredArguments.keyValue("callId", callId),
                    )
                    COUNT_CALL_EREG_ORGANISASJON_FAIL.increment()
                }
            }
            return null
        }
    }

    private fun ResponseException.isOrganisasjonNotFound(
        virksomhetsnummer: Virksomhetsnummer,
    ): Boolean {
        val is404 = this.response.status == HttpStatusCode.NotFound
        val messageNoVirksomhetsnavn =
            "Ingen organisasjon med organisasjonsnummer ${virksomhetsnummer.value} ble funnet"
        val isMessageNoVirksomhetsnavn = this.message?.contains(messageNoVirksomhetsnavn) ?: false
        return is404 && isMessageNoVirksomhetsnavn
    }

    companion object {
        const val EREG_PATH = "ereg/api/v1/organisasjon"

        const val CACHE_EREG_VIRKSOMHETSNAVN_KEY_PREFIX = "ereg-virksomhetsnavn-"
        const val CACHE_EREG_VIRKSOMHETSNAVN_TIME_TO_LIVE_SECONDS = 24 * 60 * 60L

        private val log = LoggerFactory.getLogger(EregClient::class.java)
    }
}
