package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.database.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime

@InternalAPI
object DialogmotekandidatPersonoversiktStatusApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("Get dialogmotekandidater from personoversiktstatus") {

        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = setupExternalMockEnvironment(application)
            val database = externalMockEnvironment.database
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = UserConstants.VEILEDER_ID,
            )
            beforeEachTest {
                database.connection.dropData()
            }

            describe("Get dm2-kandidater for enhet") {
                val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

                it("returns NoContent for a person with a tilfelle, who is kandidat, but has an open DM2 invitation") {
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database)
                    setDialogmotestatus(database, DialogmoteStatusendringType.INNKALT)
                    setTildeltEnhet(database)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("return NoContent for a person with a tilfelle, who is kandidat, but it's historic") {
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(365))
                    setTildeltEnhet(database)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("return NoContent for a person with a tilfelle, who is kandidat, but the delay of 7 days has NOT passed") {
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(6))
                    setTildeltEnhet(database)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("returns kandidat if they have a tilfelle, is kandidat, and a delay of 7 days has passed") {
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(10))
                    setTildeltEnhet(database)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.shouldNotBeNull()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                        personOversiktStatus
                    }
                }

                it("returns person who is kandidat if they have a tilfelle, is kandidat, and a cancelled dm2") {
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database)
                    setDialogmotestatus(database, DialogmoteStatusendringType.AVLYST)
                    setTildeltEnhet(database)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.shouldNotBeNull()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                        personOversiktStatus.motestatus shouldBeEqualTo DialogmoteStatusendringType.AVLYST.name
                    }
                }
            }
        }
    }
})
