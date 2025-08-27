package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingApiV2Path
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.api.v2.model.VeilederTildelingHistorikkDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_ACCESS
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID_2
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID_NOT_ENABLED
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate

class PersontildelingApiV2Test {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val internalMockEnvironment = InternalMockEnvironment.instance
    private val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
    private val personoversiktRepository = internalMockEnvironment.personoversiktRepository
    private val baseUrl = personTildelingApiV2Path

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = VEILEDER_ID,
    )

    @Nested
    @DisplayName("Skal lagre veiledertilknytninger")
    inner class SkalLagreVeiledertilknytninger {
        private val url = "$baseUrl/registrer"

        @Test
        fun `Skal lagre liste med veiledertilknytninger`() {
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
                    header(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString()
                    )
                    setBody("{\"tilknytninger\":[{\"veilederIdent\": \"${VEILEDER_ID}\",\"fnr\": \"${ARBEIDSTAKER_FNR}\",\"enhet\": \"${NAV_ENHET}\"}]}")
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }

    @Nested
    @DisplayName("/personer")
    inner class Personer {

        @Nested
        @DisplayName("GET veilederknytning for person")
        inner class GetVeilederknytningForPerson {

            @Test
            fun `Returns person with correct values`() {
                testApplication {
                    val client = setupApiAndClient()
                    personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                        personident = PersonIdent(
                            ARBEIDSTAKER_FNR
                        ),
                        isAktivVurdering = true,
                    )
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )
                    val tilknytning = VeilederBrukerKnytning(
                        VEILEDER_ID,
                        ARBEIDSTAKER_FNR
                    )
                    personOversiktStatusRepository.lagreVeilederForBruker(
                        tilknytning,
                        VEILEDER_ID
                    )

                    val url = "$personTildelingApiV2Path/personer/single"
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(
                            NAV_PERSONIDENT_HEADER,
                            ARBEIDSTAKER_FNR
                        )
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                    val personinfo =
                        response.body<VeilederBrukerKnytningDTO>()
                    assertEquals(tilknytning.veilederIdent, personinfo.tildeltVeilederident)
                    assertEquals(tilknytning.fnr, personinfo.personident.value)
                }
            }

            @Test
            fun `Returns 404 when person does not exist`() {
                testApplication {
                    val client = setupApiAndClient()
                    val url = "$personTildelingApiV2Path/personer/single"
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(
                            NAV_PERSONIDENT_HEADER,
                            ARBEIDSTAKER_2_FNR
                        )
                    }
                    assertEquals(HttpStatusCode.NoContent, response.status)
                }
            }
        }

        @Nested
        @DisplayName("POST veilederknytning for person")
        inner class PostVeilederknytningForPerson {
            private val url = "$personTildelingApiV2Path/personer/single"
            private val veilederBrukerKnytning = VeilederBrukerKnytning(
                VEILEDER_ID_2,
                ARBEIDSTAKER_FNR
            )

            @Test
            fun `Returns OK when request is successful`() {
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
                        header(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                        setBody(veilederBrukerKnytning)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)

                    val person =
                        personoversiktRepository.getPersonOversiktStatus(PersonIdent(veilederBrukerKnytning.fnr))
                    assertEquals(veilederBrukerKnytning.veilederIdent, person?.veilederIdent)

                    val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(
                        PersonIdent(ARBEIDSTAKER_FNR)
                    )
                    assertEquals(1, historikk.size)
                    val historikkDTO = historikk.first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, historikkDTO.tildeltVeileder)
                    assertEquals(NAV_ENHET, historikkDTO.tildeltEnhet)
                    assertEquals(VEILEDER_ID, historikkDTO.tildeltAv)
                    assertEquals(LocalDate.now(), historikkDTO.tildeltDato)
                }
            }

            @Test
            fun `Skal lagre tildeling av veileder fra tildelt til ufordelt`() {
                testApplication {
                    val ufordeltVeilederKnytning = VeilederBrukerKnytning(
                        veilederIdent = null,
                        fnr = ARBEIDSTAKER_FNR,
                    )
                    val client = setupApiAndClient()
                    personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                        personident = PersonIdent(ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )
                    database.setTildeltEnhet(ident = PersonIdent(ARBEIDSTAKER_FNR), enhet = NAV_ENHET)
                    personoversiktRepository.lagreVeilederForBruker(
                        veilederBrukerKnytning = VeilederBrukerKnytning(
                            VEILEDER_ID,
                            ARBEIDSTAKER_FNR
                        ),
                        tildeltAv = VEILEDER_ID
                    )
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        header(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                        setBody(ufordeltVeilederKnytning)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)

                    val person =
                        personoversiktRepository.getPersonOversiktStatus(PersonIdent(veilederBrukerKnytning.fnr))
                    assertNull(person?.veilederIdent)

                    val historikk =
                        personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                    assertEquals(2, historikk.size)
                }
            }

            @Test
            fun `Returns error when request sets veileder who is not active in the persons enhet`() {
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
                        header(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                        setBody(
                            VeilederBrukerKnytning(
                                VEILEDER_ID_NOT_ENABLED,
                                ARBEIDSTAKER_FNR
                            )
                        )
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                }
            }

            @Test
            fun `Returns OK when request for person som ikke finnes i oversikten is successful`() {
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        header(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                        setBody(veilederBrukerKnytning)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)

                    val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, person.veilederIdent)

                    val historikk = personOversiktStatusRepository.getVeilederTilknytningHistorikk(
                        PersonIdent(ARBEIDSTAKER_FNR)
                    )
                    assertEquals(1, historikk.size)
                    val historikkDTO = historikk.first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, historikkDTO.tildeltVeileder)
                    assertEquals(NAV_ENHET, historikkDTO.tildeltEnhet)
                    assertEquals(VEILEDER_ID, historikkDTO.tildeltAv)
                    assertEquals(LocalDate.now(), historikkDTO.tildeltDato)
                }
            }

            @Test
            fun `Returns OK when request for person som finnes i oversikten uten enhet is successful`() {
                testApplication {
                    val client = setupApiAndClient()
                    personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                        personident = PersonIdent(ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        header(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString()
                        )
                        setBody(veilederBrukerKnytning)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)

                    val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, person.veilederIdent)

                    val historikk =
                        personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                    assertEquals(1, historikk.size)
                    val historikkDTO = historikk.first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, historikkDTO.tildeltVeileder)
                    assertEquals(NAV_ENHET, historikkDTO.tildeltEnhet)
                    assertEquals(VEILEDER_ID, historikkDTO.tildeltAv)
                    assertEquals(LocalDate.now(), historikkDTO.tildeltDato)
                }
            }

            @Test
            fun `Returns OK when already assigned to veileder`() {
                personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                    personident = PersonIdent(ARBEIDSTAKER_FNR),
                    isAktivVurdering = true,
                )
                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                personoversiktRepository.lagreVeilederForBruker(
                    veilederBrukerKnytning = VeilederBrukerKnytning(
                        VEILEDER_ID_2,
                        ARBEIDSTAKER_FNR
                    ),
                    tildeltAv = VEILEDER_ID
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(veilederBrukerKnytning)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)

                    val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, person.veilederIdent)
                    val historikk =
                        personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                    assertEquals(1, historikk.size)
                }
            }

            @Test
            fun `Returns OK when already assigned to veileder and then assigned to a different`() {
                personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                    personident = PersonIdent(ARBEIDSTAKER_FNR),
                    isAktivVurdering = true,
                )
                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                personoversiktRepository.lagreVeilederForBruker(
                    veilederBrukerKnytning = VeilederBrukerKnytning(
                        VEILEDER_ID,
                        ARBEIDSTAKER_FNR
                    ),
                    tildeltAv = VEILEDER_ID
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.post(url) {
                        bearerAuth(validToken)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        setBody(veilederBrukerKnytning)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)

                    val person = database.getPersonOversiktStatusList(fnr = veilederBrukerKnytning.fnr).first()
                    assertEquals(veilederBrukerKnytning.veilederIdent, person.veilederIdent)
                    val historikk =
                        personOversiktStatusRepository.getVeilederTilknytningHistorikk(PersonIdent(ARBEIDSTAKER_FNR))
                    assertEquals(2, historikk.size)
                }
            }

            @Test
            fun `Returns Unauthorized when missing token`() {
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
                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }
            }

            @Test
            fun `Returns Forbidden when no access to person`() {
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
                        setBody(
                            VeilederBrukerKnytning(
                                VEILEDER_ID,
                                ARBEIDSTAKER_NO_ACCESS
                            )
                        )
                    }
                    assertEquals(HttpStatusCode.Forbidden, response.status)
                }
            }
        }

        @Nested
        @DisplayName("GET veilederhistorikk for person")
        inner class GetVeilederhistorikkForPerson {
            private val url = "$personTildelingApiV2Path/historikk"

            @Test
            fun `Returns OK when no tildeling`() {
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
                    assertEquals(HttpStatusCode.OK, response.status)
                    val historikk = response.body<List<VeilederTildelingHistorikkDTO>>()
                    assertEquals(emptyList<VeilederTildelingHistorikkDTO>(), historikk)
                }
            }

            @Test
            fun `Returns OK when tildeling`() {
                val personident = PersonIdent(ARBEIDSTAKER_FNR)
                personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                    personident = personident,
                    isAktivVurdering = true,
                )
                database.setTildeltEnhet(
                    ident = personident,
                    enhet = NAV_ENHET,
                )
                personoversiktRepository.lagreVeilederForBruker(
                    veilederBrukerKnytning = VeilederBrukerKnytning(
                        VEILEDER_ID,
                        ARBEIDSTAKER_FNR
                    ),
                    tildeltAv = VEILEDER_ID_2,
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                    val historikk = response.body<List<VeilederTildelingHistorikkDTO>>()
                    assertEquals(1, historikk.size)
                    val veilederHistorikkDTO = historikk.first()
                    assertEquals(VEILEDER_ID_2, veilederHistorikkDTO.tildeltAv)
                    assertEquals(VEILEDER_ID, veilederHistorikkDTO.tildeltVeileder)
                    assertEquals(NAV_ENHET, veilederHistorikkDTO.tildeltEnhet)
                    assertEquals(LocalDate.now(), veilederHistorikkDTO.tildeltDato)
                }
            }

            @Test
            fun `Returns OK when with historikk`() {
                val personident = PersonIdent(ARBEIDSTAKER_FNR)
                personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                    personident = personident,
                    isAktivVurdering = true,
                )
                database.setTildeltEnhet(
                    ident = personident,
                    enhet = NAV_ENHET,
                )
                personoversiktRepository.lagreVeilederForBruker(
                    veilederBrukerKnytning = VeilederBrukerKnytning(
                        VEILEDER_ID,
                        ARBEIDSTAKER_FNR
                    ),
                    tildeltAv = VEILEDER_ID_2,
                )
                personoversiktRepository.lagreVeilederForBruker(
                    veilederBrukerKnytning = VeilederBrukerKnytning(
                        VEILEDER_ID_2,
                        ARBEIDSTAKER_FNR
                    ),
                    tildeltAv = VEILEDER_ID,
                )
                testApplication {
                    val client = setupApiAndClient()
                    val response = client.get(url) {
                        bearerAuth(validToken)
                        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        header(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                    }
                    assertEquals(HttpStatusCode.OK, response.status)
                    val historikk = response.body<List<VeilederTildelingHistorikkDTO>>()
                    assertEquals(2, historikk.size)
                }
            }
        }
    }
}
