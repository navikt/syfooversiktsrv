package no.nav.syfo.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.domain.DialogmoteStatusendringType
import no.nav.syfo.api.endpoints.personOversiktApiV2Path
import no.nav.syfo.api.model.PersonOversiktStatusDTO
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.database.createPersonoversiktStatusWithTilfelle
import no.nav.syfo.testutil.database.setAsKandidat
import no.nav.syfo.testutil.database.setDialogmotestatus
import no.nav.syfo.testutil.database.setTildeltEnhet
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class DialogmotekandidatPersonoversiktStatusApiV2Test {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = UserConstants.VEILEDER_ID,
    )

    private val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Test
    fun `Returns NoContent for a person with a tilfelle, who is kandidat, but has an open DM2 invitation`() {
        testApplication {
            val client = setupApiAndClient()
            createPersonoversiktStatusWithTilfelle(database)
            setAsKandidat(database)
            setDialogmotestatus(database, DialogmoteStatusendringType.INNKALT)
            setTildeltEnhet(database)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Return NoContent for a person with a tilfelle, who is kandidat, but it's historic`() {
        testApplication {
            val client = setupApiAndClient()
            createPersonoversiktStatusWithTilfelle(database)
            setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(365))
            setTildeltEnhet(database)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Return NoContent for a person with a tilfelle, who is kandidat, but the delay of 7 days has NOT passed`() {
        testApplication {
            val client = setupApiAndClient()
            createPersonoversiktStatusWithTilfelle(database)
            setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(6))
            setTildeltEnhet(database)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Returns kandidat if they have a tilfelle, is kandidat, and a delay of 7 days has passed`() {
        testApplication {
            val client = setupApiAndClient()
            createPersonoversiktStatusWithTilfelle(database)
            setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(10))
            setTildeltEnhet(database)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertNotNull(personOversiktStatus)
            assertNull(personOversiktStatus.veilederIdent)
            assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertNull(personOversiktStatus.motebehovUbehandlet)
            assertNull(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertTrue(personOversiktStatus.dialogmotekandidatStatus!!.isKandidat)
            assertNull(personOversiktStatus.aktivitetskravvurdering)
        }
    }

    @Test
    fun `Returns person who is kandidat if they have a tilfelle, is kandidat, and a cancelled dm2`() {
        testApplication {
            val client = setupApiAndClient()
            createPersonoversiktStatusWithTilfelle(database)
            setAsKandidat(database)
            setDialogmotestatus(database, DialogmoteStatusendringType.AVLYST)
            setTildeltEnhet(database)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertNotNull(personOversiktStatus)
            assertNull(personOversiktStatus.veilederIdent)
            assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertNull(personOversiktStatus.motebehovUbehandlet)
            assertNull(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertTrue(personOversiktStatus.dialogmotekandidatStatus!!.isKandidat)
            assertEquals(DialogmoteStatusendringType.AVLYST.name, personOversiktStatus.motestatus)
            assertNull(personOversiktStatus.aktivitetskravvurdering)
        }
    }
}
