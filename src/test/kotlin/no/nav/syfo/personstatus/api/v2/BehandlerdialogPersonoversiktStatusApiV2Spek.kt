package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personoppgavehendelse.kafka.PersonoppgavehendelseService
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.database.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

@InternalAPI
object BehandlerdialogPersonoversiktStatusApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("Behandlerdialog fra personstatusoversikt") {

        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = setupExternalMockEnvironment(application)
            val database = externalMockEnvironment.database
            val personoppgavehendelseService = PersonoppgavehendelseService(database)
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = UserConstants.VEILEDER_ID,
            )
            val url = "$personOversiktApiV2Path/enhet/$NAV_ENHET"

            beforeEachTest {
                database.connection.dropData()
            }

            it("return person with behandlerdialog_ubehandlet true when svar mottatt") {
                val oversikthendelseBehandlerdialogSvarMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT.name,
                )
                database.connection.use {
                    personoppgavehendelseService.processPersonoppgavehendelse(
                        connection = it,
                        kPersonoppgavehendelse = oversikthendelseBehandlerdialogSvarMottatt,
                        callId = UUID.randomUUID().toString(),
                    )
                    it.commit()
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )

                with(
                    handleRequest(HttpMethod.Get, url) {
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK

                    val personOversiktStatus =
                        objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                    personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogSvarMottatt.personident
                    personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                    personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
                }
            }

            it("return person with behandlerdialog_ubehandlet true when ubesvart melding mottatt") {
                val oversikthendelseBehandlerdialogUbesvartMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT.name,
                )
                database.connection.use {
                    personoppgavehendelseService.processPersonoppgavehendelse(
                        connection = it,
                        kPersonoppgavehendelse = oversikthendelseBehandlerdialogUbesvartMottatt,
                        callId = UUID.randomUUID().toString(),
                    )
                    it.commit()
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )

                with(
                    handleRequest(HttpMethod.Get, url) {
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK

                    val personOversiktStatus =
                        objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                    personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogUbesvartMottatt.personident
                    personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                    personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
                }
            }

            it("return person with behandlerdialog_ubehandlet true when ubesvart behandlet and svar mottatt at the same time") {
                val oversikthendelseBehandlerdialogUbesvartBehandlet = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET.name,
                )
                val oversikthendelseBehandlerdialogSvarMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT.name,
                )
                database.connection.use {
                    personoppgavehendelseService.processPersonoppgavehendelse(
                        connection = it,
                        kPersonoppgavehendelse = oversikthendelseBehandlerdialogUbesvartBehandlet,
                        callId = UUID.randomUUID().toString(),
                    )
                    personoppgavehendelseService.processPersonoppgavehendelse(
                        connection = it,
                        kPersonoppgavehendelse = oversikthendelseBehandlerdialogSvarMottatt,
                        callId = UUID.randomUUID().toString(),
                    )
                    it.commit()
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )

                with(
                    handleRequest(HttpMethod.Get, url) {
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK

                    val personOversiktStatus =
                        objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                    personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogSvarMottatt.personident
                    personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                    personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
                }
            }

            it("return person with behandlerdialog_ubehandlet true when avvist melding mottatt") {
                val oversikthendelseBehandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT.name,
                )
                database.connection.use {
                    personoppgavehendelseService.processPersonoppgavehendelse(
                        connection = it,
                        kPersonoppgavehendelse = oversikthendelseBehandlerdialogAvvistMottatt,
                        callId = UUID.randomUUID().toString(),
                    )
                    it.commit()
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )

                with(
                    handleRequest(HttpMethod.Get, url) {
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK

                    val personOversiktStatus =
                        objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                    personOversiktStatus.fnr shouldBeEqualTo oversikthendelseBehandlerdialogAvvistMottatt.personident
                    personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                    personOversiktStatus.behandlerdialogUbehandlet shouldBeEqualTo true
                }
            }

            it("return no person when avvist melding behandlet") {
                val oversikthendelseBehandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                    UserConstants.ARBEIDSTAKER_FNR,
                    OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET.name,
                )
                database.connection.use {
                    personoppgavehendelseService.processPersonoppgavehendelse(
                        connection = it,
                        kPersonoppgavehendelse = oversikthendelseBehandlerdialogAvvistMottatt,
                        callId = UUID.randomUUID().toString(),
                    )
                    it.commit()
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )

                with(
                    handleRequest(HttpMethod.Get, url) {
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.NoContent
                }
            }
        }
    }
})
