package no.nav.syfo.personstatus.infrastructure.clients.pdl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.personstatus.application.IPdlClient
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlHentPersonBolkData
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlHentPersonRequest
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlHentPersonRequestVariables
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlIdentRequest
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlIdentResponse
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlIdentVariables
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlIdenter
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlPerson
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlPersonBolkRequest
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlPersonBolkResponse
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlPersonBolkVariables
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.PdlHentPersonResponse
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.errorMessage
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fullName
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class PdlClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IPdlClient {

    override suspend fun hentIdenter(
        nyPersonIdent: String,
        callId: String?,
    ): PdlIdenter? {
        val systemToken = azureAdClient.getSystemToken(clientEnvironment.clientId)
            ?: throw RuntimeException("Failed to send request to PDL: No token was found")

        val query = getPdlQuery("/pdl/hentIdenter.graphql")
        val request = PdlIdentRequest(query, PdlIdentVariables(ident = nyPersonIdent))

        val response: HttpResponse = httpClient.post(clientEnvironment.baseUrl) {
            setBody(request)
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, bearerHeader(systemToken.accessToken))
            header(BEHANDLINGSNUMMER_HEADER_KEY, BEHANDLINGSNUMMER_HEADER_VALUE)
            header(NAV_CALL_ID_HEADER, callId)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val pdlIdenterReponse = response.body<PdlIdentResponse>()
                if (!pdlIdenterReponse.errors.isNullOrEmpty()) {
                    pdlIdenterReponse.errors.forEach { error ->
                        if (error.isNotFound()) {
                            logger.warn("Error while requesting ident from PersonDataLosningen: ${error.errorMessage()}")
                        } else {
                            logger.error("Error while requesting ident from PersonDataLosningen: ${error.errorMessage()}")
                        }
                    }
                    null
                } else {
                    pdlIdenterReponse.data?.hentIdenter
                }
            }
            else -> {
                val message = "Request with url: ${clientEnvironment.baseUrl} failed with reponse code ${response.status.value}"
                logger.error(message)
                throw RuntimeException(message)
            }
        }
    }

    override suspend fun getPdlPersonIdentNumberNavnMap(
        callId: String,
        personIdentList: List<PersonIdent>,
    ): Map<String, String> =
        getPersons(
            callId = callId,
            personidenter = personIdentList,
        )
            ?.hentPersonBolk
            ?.associate { (ident, person) ->
                ident to (person?.fullName() ?: "")
            }
            ?: emptyMap()

    override suspend fun getPersons(
        callId: String?,
        personidenter: List<PersonIdent>,
    ): PdlHentPersonBolkData? {
        val token = azureAdClient.getSystemToken(clientEnvironment.clientId)
            ?: throw RuntimeException("Failed to send request to PDL: No token was found")
        val query = getPdlQuery(
            queryFilePath = "/pdl/hentPersonBolk.graphql",
        )

        val request = PdlPersonBolkRequest(
            query = query,
            variables = PdlPersonBolkVariables(
                identer = personidenter.map { personIdentNumber ->
                    personIdentNumber.value
                }
            ),
        )

        val response: HttpResponse = httpClient.post(clientEnvironment.baseUrl) {
            setBody(request)
            header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(BEHANDLINGSNUMMER_HEADER_KEY, BEHANDLINGSNUMMER_HEADER_VALUE)
            header(NAV_CALL_ID_HEADER, callId)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val pdlPersonReponse = response.body<PdlPersonBolkResponse>()
                return if (!pdlPersonReponse.errors.isNullOrEmpty()) {
                    COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                    pdlPersonReponse.errors.forEach {
                        logger.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                    }
                    null
                } else {
                    COUNT_CALL_PDL_PERSONBOLK_SUCCESS.increment()
                    pdlPersonReponse.data
                }
            }
            else -> {
                COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                logger.error("Request with url: ${clientEnvironment.baseUrl} failed with reponse code ${response.status.value}")
                return null
            }
        }
    }

    override suspend fun getPerson(personIdent: PersonIdent): Result<PdlPerson> =
        try {
            val token = azureAdClient.getSystemToken(clientEnvironment.clientId)
                ?: throw RuntimeException("Failed to send request to PDL: No token was found")
            val query = getPdlQuery("/pdl/hentPerson.graphql")
            val request = PdlHentPersonRequest(query, PdlHentPersonRequestVariables(personIdent.value))

            val response: HttpResponse = httpClient.post(clientEnvironment.baseUrl) {
                setBody(request)
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
                header(BEHANDLINGSNUMMER_HEADER_KEY, BEHANDLINGSNUMMER_HEADER_VALUE)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val pdlPersonReponse = response.body<PdlHentPersonResponse>()
                    val isErrors = !pdlPersonReponse.errors.isNullOrEmpty()
                    if (isErrors) {
                        pdlPersonReponse.errors.forEach {
                            logger.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                        }
                    }
                    pdlPersonReponse.data?.hentPerson
                        ?.let { Result.success(it) }
                        ?: Result.failure(RuntimeException("No person found in PDL response"))
                }

                else -> {
                    logger.error("Request with url: ${clientEnvironment.baseUrl} failed with reponse code ${response.status.value}")
                    Result.failure(RuntimeException("Failed to get person from PDL"))
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to get person from PDL", e)
            Result.failure(e)
        }

    private fun getPdlQuery(queryFilePath: String): String {
        return this::class.java.getResource(queryFilePath)!!
            .readText()
            .replace("[\n\r]", "")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PdlClient::class.java)

        // Se behandlingskatalog https://behandlingskatalog.intern.nav.no/
        // Behandling: Sykefraværsoppfølging: Vurdere behov for oppfølging og rett til sykepenger etter §§ 8-4 og 8-8
        private const val BEHANDLINGSNUMMER_HEADER_KEY = "behandlingsnummer"
        private const val BEHANDLINGSNUMMER_HEADER_VALUE = "B426"
    }
}
