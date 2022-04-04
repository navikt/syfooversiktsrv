package no.nav.syfo.client.ereg

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class EregClient(
    private val azureAdClient: AzureAdClient,
    private val isproxyClientId: String,
    baseUrl: String,
    private val redisStore: RedisStore,
) {
    private val httpClient = httpClientDefault()

    private val eregOrganisasjonUrl: String = "$baseUrl/$EREG_PATH"

    suspend fun organisasjonVirksomhetsnavn(
        callId: String,
        virksomhetsnummer: Virksomhetsnummer,
    ): EregVirksomhetsnavn? {
        val cacheKey = "${CACHE_EREG_VIRKSOMHETSNAVN_KEY_PREFIX}${virksomhetsnummer.value}"
        val cachedResponse: EregVirksomhetsnavn? = redisStore.getObject(key = cacheKey)

        if (cachedResponse != null) {
            return cachedResponse
        } else {
            val systemToken = azureAdClient.getSystemToken(
                scopeClientId = isproxyClientId,
            )?.accessToken
                ?: throw RuntimeException("Failed to request Organisasjon from Isproxy-Ereg: Failed to get system token from AzureAD")

            try {
                val url = "$eregOrganisasjonUrl/${virksomhetsnummer.value}"
                val response: EregOrganisasjonResponse = httpClient.get(url) {
                    header(HttpHeaders.Authorization, bearerHeader(systemToken))
                    header(NAV_CALL_ID_HEADER, callId)
                    accept(ContentType.Application.Json)
                }
                COUNT_CALL_EREG_ORGANISASJON_SUCCESS.increment()
                val eregVirksomhetsnavn = response.toEregVirksomhetsnavn()
                redisStore.setObject(
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
        const val EREG_PATH = "/api/v1/ereg/organisasjon"

        const val CACHE_EREG_VIRKSOMHETSNAVN_KEY_PREFIX = "ereg-virksomhetsnavn-"
        const val CACHE_EREG_VIRKSOMHETSNAVN_TIME_TO_LIVE_SECONDS = 12 * 60 * 60L

        private val log = LoggerFactory.getLogger(EregClient::class.java)
    }
}
