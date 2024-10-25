package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingApiV2Path
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_ACCESS
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID_2
import no.nav.syfo.testutil.database.setTildeltEnhet
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

@InternalAPI
object PersontildelingApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("PersontildelingApi") {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val internalMockEnvironment = InternalMockEnvironment.instance
            val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
            val personOversiktStatusRepository = PersonOversiktStatusRepository(database)
            val personTildelingService = internalMockEnvironment.personTildelingService

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
                    personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                        personident = PersonIdent(ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )
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
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(ARBEIDSTAKER_FNR),
                            enhet = NAV_ENHET,
                        )
                        val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
                        personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, VEILEDER_ID)

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
                    val veilederBrukerKnytning = VeilederBrukerKnytning(VEILEDER_ID_2, ARBEIDSTAKER_FNR)

                    it("returns OK when request is successful") {
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(ARBEIDSTAKER_FNR),
                            enhet = NAV_ENHET,
                        )
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

                            val historikk = database.getVeilederHistorikk(ARBEIDSTAKER_FNR)
                            historikk.size shouldBeEqualTo 1
                            val historikkDTO = historikk.first()
                            historikkDTO.tildeltVeileder shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                            historikkDTO.tildeltEnhet shouldBeEqualTo NAV_ENHET
                            historikkDTO.tildeltAv shouldBeEqualTo VEILEDER_ID
                            historikkDTO.fraDato shouldBeEqualTo LocalDate.now()
                        }
                    }
                    it("returns OK when request for person som ikke finnes i oversikten is successful") {
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                setBody(objectMapper.writeValueAsString(veilederBrukerKnytning))
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val person =
                                database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                            person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent

                            val historikk = database.getVeilederHistorikk(ARBEIDSTAKER_FNR)
                            historikk.size shouldBeEqualTo 1
                            val historikkDTO = historikk.first()
                            historikkDTO.tildeltVeileder shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                            historikkDTO.tildeltEnhet shouldBeEqualTo NAV_ENHET
                            historikkDTO.tildeltAv shouldBeEqualTo VEILEDER_ID
                            historikkDTO.fraDato shouldBeEqualTo LocalDate.now()
                        }
                    }
                    it("returns OK when already assigned to veileder") {
                        runBlocking {
                            personTildelingService.lagreKnytningMellomVeilederOgBruker(
                                listOf(VeilederBrukerKnytning(VEILEDER_ID_2, ARBEIDSTAKER_FNR)),
                                VEILEDER_ID
                            )
                        }
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
                            val historikk = database.getVeilederHistorikk(ARBEIDSTAKER_FNR)
                            historikk.size shouldBeEqualTo 1
                        }
                    }
                    it("returns OK when already assigned to veileder and then assigned to a different") {
                        runBlocking {
                            personTildelingService.lagreKnytningMellomVeilederOgBruker(
                                listOf(VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)),
                                VEILEDER_ID
                            )
                        }
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
                            val historikk = database.getVeilederHistorikk(ARBEIDSTAKER_FNR)
                            historikk.size shouldBeEqualTo 2
                        }
                    }

                    it("returns Unauthorized when missing token") {
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(ARBEIDSTAKER_FNR),
                            enhet = NAV_ENHET,
                        )
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
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        setTildeltEnhet(database)
                        with(
                            handleRequest(HttpMethod.Post, url) {
                                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                                setBody(
                                    objectMapper.writeValueAsString(
                                        VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_NO_ACCESS)
                                    )
                                )
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
