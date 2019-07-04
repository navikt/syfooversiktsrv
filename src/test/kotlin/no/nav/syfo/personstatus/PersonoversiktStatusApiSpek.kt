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
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

private val env = getEnvironment()

@InternalAPI
object PersonoversiktStatusApiSpek : Spek({


    val database by lazy { TestDB() }
    val cookies = ""
    val baseUrl = "/api/v1/personoversikt"
    val tilgangskontrollConsumer = TilgangskontrollConsumer(
            env.syfotilgangskontrollUrl,
            client
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
                registerPersonoversiktApi(tilgangskontrollConsumer, PersonoversiktStatusService(database))
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

                it("skal hente enhet sin personoversikt ") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val returnertVerdig = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!)[0]
                        returnertVerdig.veilederIdent shouldEqual tilknytning.veilederIdent
                        returnertVerdig.fnr shouldEqual tilknytning.fnr
                        returnertVerdig.enhet shouldEqual tilknytning.enhet
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

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"
