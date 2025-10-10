package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehandlerdialogPersonoversiktStatusApiV2Test {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = UserConstants.VEILEDER_ID,
    )
    private val url = "$personOversiktApiV2Path/enhet/$NAV_ENHET"

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Test
    fun `Return person with behandlerdialog_ubehandlet true when svar mottatt`() {
        testApplication {
            val client = setupApiAndClient()

            val oversikthendelseBehandlerdialogSvarMottatt = KPersonoppgavehendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversikthendelseBehandlerdialogSvarMottatt.personident
            ).applyOversikthendelse(oversikthendelseBehandlerdialogSvarMottatt.hendelsetype)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )

            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(oversikthendelseBehandlerdialogSvarMottatt.personident, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.behandlerdialogUbehandlet)
        }
    }

    @Test
    fun `Return person with behandlerdialog_ubehandlet true when ubesvart melding mottatt`() {
        testApplication {
            val client = setupApiAndClient()
            val oversikthendelseBehandlerdialogUbesvartMottatt = KPersonoppgavehendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversikthendelseBehandlerdialogUbesvartMottatt.personident
            ).applyOversikthendelse(oversikthendelseBehandlerdialogUbesvartMottatt.hendelsetype)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)

            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(oversikthendelseBehandlerdialogUbesvartMottatt.personident, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.behandlerdialogUbehandlet)
        }
    }

    @Test
    fun `Return person with behandlerdialog_ubehandlet true when ubesvart behandlet and svar mottatt at the same time`() {
        testApplication {
            val client = setupApiAndClient()
            val oversikthendelseBehandlerdialogUbesvartBehandlet = KPersonoppgavehendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET,
            )
            val oversikthendelseBehandlerdialogSvarMottatt = KPersonoppgavehendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversikthendelseBehandlerdialogSvarMottatt.personident
            )
                .applyOversikthendelse(oversikthendelseBehandlerdialogUbesvartBehandlet.hendelsetype)
                .applyOversikthendelse(oversikthendelseBehandlerdialogSvarMottatt.hendelsetype)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)

            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(oversikthendelseBehandlerdialogSvarMottatt.personident, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.behandlerdialogUbehandlet)
        }
    }

    @Test
    fun `Return person with behandlerdialog_ubehandlet true when avvist melding mottatt`() {
        testApplication {
            val client = setupApiAndClient()
            val oversikthendelseBehandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversikthendelseBehandlerdialogAvvistMottatt.personident
            ).applyOversikthendelse(oversikthendelseBehandlerdialogAvvistMottatt.hendelsetype)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)

            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(oversikthendelseBehandlerdialogAvvistMottatt.personident, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.behandlerdialogUbehandlet)
        }
    }

    @Test
    fun `Return no person when avvist melding behandlet`() {
        testApplication {
            val client = setupApiAndClient()
            val oversikthendelseBehandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                UserConstants.ARBEIDSTAKER_FNR,
                OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversikthendelseBehandlerdialogAvvistMottatt.personident
            ).applyOversikthendelse(oversikthendelseBehandlerdialogAvvistMottatt.hendelsetype)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }
}
