package no.nav.syfo.personstatus.api.system

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingSystemApiPath
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.InternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generateJWT
import no.nav.syfo.testutil.setTildeltEnhet
import no.nav.syfo.testutil.setupApiAndClient
import no.nav.syfo.testutil.testAzureAppPreAuthorizedApps
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersontildelingSystemApiTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val internalMockEnvironment = InternalMockEnvironment.Companion.instance
    private val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
    private val baseUrl = personTildelingSystemApiPath

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    private val azp = testAzureAppPreAuthorizedApps.find { preAuthorizedClient ->
        preAuthorizedClient.clientId.contains("syfobehandlendeenhet")
    }?.clientId ?: throw RuntimeException("Failed to find azp for syfobehandlendeenhet")

    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        azp = azp,
    )

    @Test
    fun `Returns person with correct values`() {
        testApplication {
            val client = setupApiAndClient()
            personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                isAktivVurdering = true,
            )
            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = UserConstants.NAV_ENHET,
            )
            val tilknytning =
                VeilederBrukerKnytning(UserConstants.VEILEDER_ID, UserConstants.ARBEIDSTAKER_FNR)
            personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, UserConstants.VEILEDER_ID)

            val url = "$baseUrl/personer/single"
            val response = client.get(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR)
            }
            assertEquals(HttpStatusCode.Companion.OK, response.status)
            val personinfo = response.body<VeilederBrukerKnytningDTO>()
            assertEquals(tilknytning.veilederIdent, personinfo.tildeltVeilederident)
            assertEquals(tilknytning.fnr, personinfo.personident.value)
        }
    }

    @Test
    fun `Returns 404 when person does not exist`() {
        testApplication {
            val client = setupApiAndClient()
            val url = "$baseUrl/personer/single"
            val response = client.get(url) {
                bearerAuth(validToken)
                header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_2_FNR)
            }
            assertEquals(HttpStatusCode.Companion.NoContent, response.status)
        }
    }

    @Test
    fun `Returns Unauthorized when missing token`() {
        testApplication {
            val client = setupApiAndClient()
            personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                isAktivVurdering = true,
            )
            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = UserConstants.NAV_ENHET,
            )
            val url = "$baseUrl/personer/single"
            val response = client.get(url) {
                header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_2_FNR)
            }
            assertEquals(HttpStatusCode.Companion.Unauthorized, response.status)
        }
    }

    @Test
    fun `Returns Forbidden when unauhorized application`() {
        testApplication {
            val validTokenUnknownAzp = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                azp = "dev-gcp.teamsykefravr.ishuskelapp",
            )

            val client = setupApiAndClient()
            personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                isAktivVurdering = true,
            )
            no.nav.syfo.testutil.database.setTildeltEnhet(database)
            val url = "$baseUrl/personer/single"
            val response = client.get(url) {
                bearerAuth(validTokenUnknownAzp)
                header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR)
            }
            assertEquals(HttpStatusCode.Companion.Forbidden, response.status)
        }
    }
}
