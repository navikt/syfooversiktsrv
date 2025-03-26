package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.applyHendelse
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BehandlerdialogPersonoversiktStatusApiV2Spek : Spek({
    describe("Behandlerdialog fra personstatusoversikt") {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azure.appClientId,
            issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
            navIdent = UserConstants.VEILEDER_ID,
        )
        val url = "$personOversiktApiV2Path/enhet/$NAV_ENHET"

        beforeEachTest {
            database.dropData()
        }

        it("return person with behandlerdialog_ubehandlet true when svar mottatt") {
            testApplication {
                val client = setupApiAndClient()

                val oversikthendelseBehandlerdialogSvarMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT,
                )
                val personoversiktStatus = PersonOversiktStatus(
                    fnr = oversikthendelseBehandlerdialogSvarMottatt.personident
                ).applyHendelse(oversikthendelseBehandlerdialogSvarMottatt.hendelsetype)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )

                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK
                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogSvarMottatt.personident
                personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO.enhetId
                personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
            }
        }

        it("return person with behandlerdialog_ubehandlet true when ubesvart melding mottatt") {
            testApplication {
                val client = setupApiAndClient()
                val oversikthendelseBehandlerdialogUbesvartMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT,
                )
                val personoversiktStatus = PersonOversiktStatus(
                    fnr = oversikthendelseBehandlerdialogUbesvartMottatt.personident
                ).applyHendelse(oversikthendelseBehandlerdialogUbesvartMottatt.hendelsetype)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK

                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogUbesvartMottatt.personident
                personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO.enhetId
                personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
            }
        }

        it("return person with behandlerdialog_ubehandlet true when ubesvart behandlet and svar mottatt at the same time") {
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
                    .applyHendelse(oversikthendelseBehandlerdialogUbesvartBehandlet.hendelsetype)
                    .applyHendelse(oversikthendelseBehandlerdialogSvarMottatt.hendelsetype)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK

                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogSvarMottatt.personident
                personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO.enhetId
                personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
            }
        }

        it("return person with behandlerdialog_ubehandlet true when avvist melding mottatt") {
            testApplication {
                val client = setupApiAndClient()
                val oversikthendelseBehandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT,
                )
                val personoversiktStatus = PersonOversiktStatus(
                    fnr = oversikthendelseBehandlerdialogAvvistMottatt.personident
                ).applyHendelse(oversikthendelseBehandlerdialogAvvistMottatt.hendelsetype)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK

                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogAvvistMottatt.personident
                personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO.enhetId
                personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
            }
        }

        it("return no person when avvist melding behandlet") {
            testApplication {
                val client = setupApiAndClient()
                val oversikthendelseBehandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET,
                )
                val personoversiktStatus = PersonOversiktStatus(
                    fnr = oversikthendelseBehandlerdialogAvvistMottatt.personident
                ).applyHendelse(oversikthendelseBehandlerdialogAvvistMottatt.hendelsetype)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                response.status shouldBeEqualTo HttpStatusCode.NoContent
            }
        }
    }
})
