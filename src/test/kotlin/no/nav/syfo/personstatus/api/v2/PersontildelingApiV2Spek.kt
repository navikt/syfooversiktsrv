package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingApiV2Path
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.api.v2.model.VeilederTildelingHistorikkDTO
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_ACCESS
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID_2
import no.nav.syfo.testutil.database.setTildeltEnhet
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

object PersontildelingApiV2Spek : Spek({

    describe("PersontildelingApi") {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
        val internalMockEnvironment = InternalMockEnvironment.instance
        val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
        val personTildelingService = internalMockEnvironment.personTildelingService
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
                testApplication {
                    val client = setupApiAndClient()
                    personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                        personident = PersonIdent(ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody("{\"tilknytninger\":[{\"veilederIdent\": \"$VEILEDER_ID\",\"fnr\": \"$ARBEIDSTAKER_FNR\",\"enhet\": \"$NAV_ENHET\"}]}")
                    }
                    response.status shouldBeEqualTo HttpStatusCode.OK
                }
            }
        }

        describe("/personer") {
            describe("GET veilederknytning for person") {
                it("returns person with correct values") {
                    testApplication {
                        val client = setupApiAndClient()
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
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val personinfo = response.body<VeilederBrukerKnytningDTO>()
                        personinfo.tildeltVeilederident shouldBeEqualTo tilknytning.veilederIdent
                        personinfo.personident.value shouldBeEqualTo tilknytning.fnr
                    }
                }
                it("returns 404 when person does not exist") {
                    testApplication {
                        val client = setupApiAndClient()
                        val url = "$personTildelingApiV2Path/personer/single"
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_2_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
            }
            describe("POST veilederknytning for person") {
                val url = "$personTildelingApiV2Path/personer/single"
                val veilederBrukerKnytning = VeilederBrukerKnytning(VEILEDER_ID_2, ARBEIDSTAKER_FNR)

                it("returns OK when request is successful") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(ARBEIDSTAKER_FNR),
                            enhet = NAV_ENHET,
                        )
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(veilederBrukerKnytning)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                        person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent

                        val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                        historikk.size shouldBeEqualTo 1
                        val historikkDTO = historikk.first()
                        historikkDTO.tildeltVeileder shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        historikkDTO.tildeltEnhet shouldBeEqualTo NAV_ENHET
                        historikkDTO.tildeltAv shouldBeEqualTo VEILEDER_ID
                        historikkDTO.tildeltDato shouldBeEqualTo LocalDate.now()
                    }
                }
                it("returns OK when request for person som ikke finnes i oversikten is successful") {
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(veilederBrukerKnytning)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                        person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent

                        val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                        historikk.size shouldBeEqualTo 1
                        val historikkDTO = historikk.first()
                        historikkDTO.tildeltVeileder shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        historikkDTO.tildeltEnhet shouldBeEqualTo NAV_ENHET
                        historikkDTO.tildeltAv shouldBeEqualTo VEILEDER_ID
                        historikkDTO.tildeltDato shouldBeEqualTo LocalDate.now()
                    }
                }
                it("returns OK when request for person som finnes i oversikten uten enhet is successful") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(veilederBrukerKnytning)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val person =
                            database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                        person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent

                        val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                        historikk.size shouldBeEqualTo 1
                        val historikkDTO = historikk.first()
                        historikkDTO.tildeltVeileder shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        historikkDTO.tildeltEnhet shouldBeEqualTo NAV_ENHET
                        historikkDTO.tildeltAv shouldBeEqualTo VEILEDER_ID
                        historikkDTO.tildeltDato shouldBeEqualTo LocalDate.now()
                    }
                }
                it("returns OK when already assigned to veileder") {
                    runBlocking {
                        personTildelingService.lagreKnytningMellomVeilederOgBruker(
                            listOf(VeilederBrukerKnytning(VEILEDER_ID_2, ARBEIDSTAKER_FNR)),
                            VEILEDER_ID
                        )
                    }
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(veilederBrukerKnytning)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                        person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
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
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(veilederBrukerKnytning)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK

                        val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                        person.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                        historikk.size shouldBeEqualTo 2
                    }
                }

                it("returns Unauthorized when missing token") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(ARBEIDSTAKER_FNR),
                            enhet = NAV_ENHET,
                        )
                        val response = client.post(url) {
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(veilederBrukerKnytning)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.Unauthorized
                    }
                }

                it("returns Forbidden when no access to person") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        setTildeltEnhet(database)
                        val response = client.post(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            setBody(VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_NO_ACCESS))
                        }
                        response.status shouldBeEqualTo HttpStatusCode.Forbidden
                    }
                }
            }
            describe("GET veilederhistorikk for person") {
                val url = "$personTildelingApiV2Path/historikk"

                it("returns OK when no tildeling") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val historikk = response.body<List<VeilederTildelingHistorikkDTO>>()
                        historikk shouldBeEqualTo emptyList()
                    }
                }
                it("returns OK when tildeling") {
                    runBlocking {
                        personTildelingService.lagreKnytningMellomVeilederOgBruker(
                            listOf(VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)),
                            VEILEDER_ID_2
                        )
                    }
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val historikk = response.body<List<VeilederTildelingHistorikkDTO>>()
                        historikk.size shouldBeEqualTo 1
                        val veilederHistorikkDTO = historikk.first()
                        veilederHistorikkDTO.tildeltAv shouldBeEqualTo VEILEDER_ID_2
                        veilederHistorikkDTO.tildeltVeileder shouldBeEqualTo VEILEDER_ID
                        veilederHistorikkDTO.tildeltEnhet shouldBeEqualTo NAV_ENHET
                        veilederHistorikkDTO.tildeltDato shouldBeEqualTo LocalDate.now()
                    }
                }
                it("returns OK when with historikk") {
                    runBlocking {
                        personTildelingService.lagreKnytningMellomVeilederOgBruker(
                            listOf(VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)),
                            VEILEDER_ID_2
                        )
                        personTildelingService.lagreKnytningMellomVeilederOgBruker(
                            listOf(VeilederBrukerKnytning(VEILEDER_ID_2, ARBEIDSTAKER_FNR)),
                            VEILEDER_ID
                        )
                    }
                    testApplication {
                        val client = setupApiAndClient()
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.OK
                        val historikk = response.body<List<VeilederTildelingHistorikkDTO>>()
                        historikk.size shouldBeEqualTo 2
                    }
                }
            }
        }
    }
})
