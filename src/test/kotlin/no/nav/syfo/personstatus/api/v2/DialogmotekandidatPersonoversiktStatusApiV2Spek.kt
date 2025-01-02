package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.database.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime

object DialogmotekandidatPersonoversiktStatusApiV2Spek : Spek({
    describe("Get dialogmotekandidater from personoversiktstatus") {

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azure.appClientId,
            issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
            navIdent = UserConstants.VEILEDER_ID,
        )

        beforeEachTest {
            database.dropData()
        }

        describe("Get dm2-kandidater for enhet") {
            val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

            it("returns NoContent for a person with a tilfelle, who is kandidat, but has an open DM2 invitation") {
                testApplication {
                    val client = setupApiAndClient()
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database)
                    setDialogmotestatus(database, DialogmoteStatusendringType.INNKALT)
                    setTildeltEnhet(database)
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("return NoContent for a person with a tilfelle, who is kandidat, but it's historic") {
                testApplication {
                    val client = setupApiAndClient()
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(365))
                    setTildeltEnhet(database)
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("return NoContent for a person with a tilfelle, who is kandidat, but the delay of 7 days has NOT passed") {
                testApplication {
                    val client = setupApiAndClient()
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(6))
                    setTildeltEnhet(database)
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("returns kandidat if they have a tilfelle, is kandidat, and a delay of 7 days has passed") {
                testApplication {
                    val client = setupApiAndClient()
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database, kandidatGeneratedAt = OffsetDateTime.now().minusDays(10))
                    setTildeltEnhet(database)
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                    personOversiktStatus.shouldNotBeNull()
                    personOversiktStatus.veilederIdent shouldBeEqualTo null
                    personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                    personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                    personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                    personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                    personOversiktStatus.aktivitetskravvurdering.shouldBeNull()
                }
            }

            it("returns person who is kandidat if they have a tilfelle, is kandidat, and a cancelled dm2") {
                testApplication {
                    val client = setupApiAndClient()
                    createPersonoversiktStatusWithTilfelle(database)
                    setAsKandidat(database)
                    setDialogmotestatus(database, DialogmoteStatusendringType.AVLYST)
                    setTildeltEnhet(database)
                    val response = client.get(url) {
                        bearerAuth(validToken)
                    }
                    response.status shouldBeEqualTo HttpStatusCode.OK
                    val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                    personOversiktStatus.shouldNotBeNull()
                    personOversiktStatus.veilederIdent shouldBeEqualTo null
                    personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                    personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                    personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                    personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                    personOversiktStatus.motestatus shouldBeEqualTo DialogmoteStatusendringType.AVLYST.name
                    personOversiktStatus.aktivitetskravvurdering.shouldBeNull()
                }
            }
        }
    }
})
