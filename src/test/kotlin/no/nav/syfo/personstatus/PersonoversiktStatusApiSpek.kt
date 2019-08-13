package no.nav.syfo.personstatus

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.ContentNegotiation
import io.ktor.http.*
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.InternalAPI
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.io.ByteReadChannel
import no.nav.syfo.auth.getTokenFromCookie
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.getEnvironment
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ETTERNAVN
import no.nav.syfo.testutil.UserConstants.VEILEDER_FORNAVN
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

private val env = getEnvironment()

@InternalAPI
object PersonoversiktStatusApiSpek : Spek({

    val database = TestDB()
    val cookies = ""
    val baseUrl = "/api/v1/personoversikt"
    val tilgangskontrollConsumer = TilgangskontrollConsumer(
            env.syfotilgangskontrollUrl,
            client
    )
    val oversiktHendelseService = OversiktHendelseService(database)
    val veilederConsumer = VeilederConsumer(
            env.syfoveilederUrl,
            clientVeileder
    )

    afterGroup {
        database.stop()
    }

    describe("PersonoversiktApi") {

        with(TestApplicationEngine()) {
            start()

            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }

            application.routing {
                registerPersonoversiktApi(tilgangskontrollConsumer, PersonoversiktStatusService(database, veilederConsumer))
            }

            beforeEachTest {
                mockkStatic("no.nav.syfo.auth.TokenAuthKt")
            }

            afterEachTest {
                database.connection.dropData()
            }

            describe("Hent personoversikt for enhet") {
                val url = "$baseUrl/enhet/$NAV_ENHET"

                it("skal returnere status Unauthorized uten gyldig id-token i cookies") {
                    every {
                        isInvalidToken(any())
                    } returns true

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.Unauthorized
                    }
                }

                it("skal returnere status NoContent om det ikke er noen personer som er tilknyttet enhet") {
                    every {
                        isInvalidToken(any())
                    } returns false

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("skal hente enhet sin personoversikt med ubehandlet motebehovsvar") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!)[0]
                        personOversiktStatus.veilederIdent shouldEqual tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldEqual oversiktHendelse.fnr
                        personOversiktStatus.enhet shouldEqual oversiktHendelse.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual true
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual null
                        personOversiktStatus.veileder?.fornavn shouldEqual VEILEDER_FORNAVN
                        personOversiktStatus.veileder?.etternavn shouldEqual VEILEDER_ETTERNAVN
                    }
                }

                it("skal hente enhet sin personoversikt med ubehandlet moteplanleggersvar") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!)[0]
                        personOversiktStatus.veilederIdent shouldEqual tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldEqual oversiktHendelse.fnr
                        personOversiktStatus.enhet shouldEqual oversiktHendelse.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual null
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual true
                        personOversiktStatus.veileder?.fornavn shouldEqual VEILEDER_FORNAVN
                        personOversiktStatus.veileder?.etternavn shouldEqual VEILEDER_ETTERNAVN
                    }
                }

                it("skal hente returnere NoContent, om alle personer i personoversikt er behandlet") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseNy = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseMoteplanleggerBehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerBehandlet)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }
            }
        }
    }
})

@InternalAPI
private val client = HttpClient(MockEngine) {
    val baseUrl = env.syfotilgangskontrollUrl
    engine {
        addHandler { request ->
            when (request.url.fullUrl) {
                "$baseUrl/syfo-tilgangskontroll/api/tilgang/enhet?enhet=$NAV_ENHET" -> {
                    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(ByteReadChannel(("{" +
                            "\"harTilgang\":\"true\",\"begrunnelse\":\"null\"}").toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                "$baseUrl/syfo-tilgangskontroll/api/tilgang/bruker?fnr=$ARBEIDSTAKER_FNR" -> {
                    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(ByteReadChannel(("{" +
                            "\"harTilgang\":\"true\",\"begrunnelse\":\"null\"}").toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                else -> error("Unhandled ${request.url.fullUrl}")
            }
        }
    }
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

@InternalAPI
private val clientVeileder = HttpClient(MockEngine) {
    val baseUrl = env.syfoveilederUrl
    engine {
        addHandler { request ->
            when (request.url.fullUrl) {
                "$baseUrl/syfoveileder/api/veiledere/enhet/$NAV_ENHET" -> {
                    val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                    respond(ByteReadChannel((
                            "[{\"ident\":\"Z999999\",\"fornavn\":\"$VEILEDER_FORNAVN\"," +
                                    "\"etternavn\":\"$VEILEDER_ETTERNAVN\",\"enhetNr\":\"$NAV_ENHET\",\"enhetNavn\":\"NAV X-FILES\"}]"

                            )
                            .toByteArray(Charsets.UTF_8)), HttpStatusCode.OK, responseHeaders)
                }
                else -> error("Unhandled ${request.url.fullUrl}")
            }
        }
    }
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
}

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"
