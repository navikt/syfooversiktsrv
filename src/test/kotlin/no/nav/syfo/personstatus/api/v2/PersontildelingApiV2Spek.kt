package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingApiV2Path
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_ACCESS
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
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

            describe("/personer") {
                describe("GET veilederknytning for person") {
                    it("returns person with correct values") {
                        val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
                        database.lagreVeilederForBruker(tilknytning)

                        val url = "$personTildelingApiV2Path/personer/single"
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            val personinfo = objectMapper.readValue<VeilederBrukerKnytningDTO>(response.content!!)
                            personinfo.tildeltVeilederident shouldBeEqualTo tilknytning.veilederIdent
                            personinfo.personident.value shouldBeEqualTo tilknytning.fnr
                        }
                    }
                    it("returns 404 when person does not exist") {
                        val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
                        database.lagreVeilederForBruker(tilknytning)

                        val url = "$personTildelingApiV2Path/personer/single"
                        with(
                            handleRequest(HttpMethod.Get, url) {
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                addHeader(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_2_FNR)
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.NoContent
                        }
                    }
                }
                describe("POST veilederknytning for person") {
                    val url = "$personTildelingApiV2Path/personer/single"

                    val veilederBrukerKnytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)

                    it("returns OK when request is successful") {
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                setBody(objectMapper.writeValueAsString(veilederBrukerKnytning))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                            person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        }
                    }

                    it("returns Unauthorized when missing token") {
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                setBody(objectMapper.writeValueAsString(veilederBrukerKnytning))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                        }
                    }

                    it("returns Forbidden when no access to person") {
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                setBody(objectMapper.writeValueAsString(VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_NO_ACCESS)))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }
    }
})
