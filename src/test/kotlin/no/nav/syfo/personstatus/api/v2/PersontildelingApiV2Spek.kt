package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingApiV2Path
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object PersontildelingApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("PersontildelingApi") {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment
            )

            val baseUrl = personTildelingApiV2Path

            beforeEachTest {
                database.dropData()
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )

            describe("Hent veiledertilknytninger") {
                val url = "$baseUrl/veileder/$VEILEDER_ID"

                it("skal returnere status NoContent om veileder ikke har tilknytninger") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("skal hente veileder sine tilknytninger ") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.lagreVeilederForBruker(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val returnertVerdi =
                            objectMapper.readValue<List<VeilederBrukerKnytning>>(response.content!!)[0]
                        returnertVerdi.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        returnertVerdi.fnr shouldBeEqualTo tilknytning.fnr
                        returnertVerdi.enhet shouldBeEqualTo ""
                    }
                }
            }

            describe("skal lagre veiledertilknytninger") {
                val url = "$baseUrl/registrer"

                it("skal lagre liste med veiledertilknytninger") {

                    with(
                        handleRequest(HttpMethod.Post, url) {
                            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                            setBody("{\"tilknytninger\":[{\"veilederIdent\": \"$VEILEDER_ID\",\"fnr\": \"$ARBEIDSTAKER_FNR\",\"enhet\": \"$NAV_ENHET\"}]}")
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                    }
                }
            }
        }
    }
})
