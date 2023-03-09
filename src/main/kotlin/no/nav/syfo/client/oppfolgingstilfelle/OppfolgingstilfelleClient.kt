package no.nav.syfo.client.oppfolgingstilfelle

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.*
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import java.util.UUID

class OppfolgingstilfelleClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment
) {
    private val personOppfolgingstilfelleSystemUrl: String =
        "${clientEnvironment.baseUrl}$ISOPPFOLGINGSTILFELLE_OPPFOLGINGSTILFELLE_SYSTEM_PERSON_PATH"

    private val httpClient = httpClientDefault()

    suspend fun getOppfolgingstilfellePerson(
        personIdent: PersonIdent,
    ): OppfolgingstilfellePersonDTO? {
        val callIdToUse = UUID.randomUUID().toString()
        return try {
            val token = azureAdClient.getSystemToken(clientEnvironment.clientId)
                ?: throw RuntimeException("Could not get azuread access token")
            val response: HttpResponse = httpClient.get(personOppfolgingstilfelleSystemUrl) {
                header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
                header(NAV_CALL_ID_HEADER, callIdToUse)
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                accept(ContentType.Application.Json)
            }
            response.body<OppfolgingstilfellePersonDTO>()
        } catch (responseException: ResponseException) {
            log.error(
                "Error while requesting OppfolgingstilfellePerson from Isoppfolgingstilfelle with {}, {}",
                StructuredArguments.keyValue("statusCode", responseException.response.status.value),
                callIdArgument(callIdToUse),
            )
            throw responseException
        }
    }

    companion object {
        const val ISOPPFOLGINGSTILFELLE_OPPFOLGINGSTILFELLE_SYSTEM_PERSON_PATH =
            "/api/system/v1/oppfolgingstilfelle/personident"

        private val log = LoggerFactory.getLogger(OppfolgingstilfelleClient::class.java)
    }
}
