package no.nav.syfo.personstatus.api.system

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.syfo.personstatus.api.v2.endpoints.personTildelingSystemApiPath
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.InternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generateJWT
import no.nav.syfo.testutil.setTildeltEnhet
import no.nav.syfo.testutil.setupApiAndClient
import no.nav.syfo.testutil.testAzureAppPreAuthorizedApps
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PersontildelingSystemApiSpek : Spek({

    describe("PersontildelingApi") {
        val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
        val database = externalMockEnvironment.database
        val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
        val internalMockEnvironment = InternalMockEnvironment.Companion.instance
        val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
        val baseUrl = personTildelingSystemApiPath

        beforeEachTest {
            database.dropData()
        }
        val azp = testAzureAppPreAuthorizedApps.find { preAuthorizedClient ->
            preAuthorizedClient.clientId.contains("syfobehandlendeenhet")
        }?.clientId ?: throw RuntimeException("Failed to find azp for syfobehandlendeenhet")

        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azure.appClientId,
            issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
            azp = azp,
        )

        describe("/personer/single") {
            describe("GET veilederknytning for person") {
                it("returns person with correct values") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                            enhet = UserConstants.NAV_ENHET,
                        )
                        val tilknytning =
                            VeilederBrukerKnytning(UserConstants.VEILEDER_ID, UserConstants.ARBEIDSTAKER_FNR)
                        personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, UserConstants.VEILEDER_ID)

                        val url = "$baseUrl/personer/single"
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.Companion.OK
                        val personinfo = response.body<VeilederBrukerKnytningDTO>()
                        personinfo.tildeltVeilederident shouldBeEqualTo tilknytning.veilederIdent
                        personinfo.personident.value shouldBeEqualTo tilknytning.fnr
                    }
                }
                it("returns 404 when person does not exist") {
                    testApplication {
                        val client = setupApiAndClient()
                        val url = "$baseUrl/personer/single"
                        val response = client.get(url) {
                            bearerAuth(validToken)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_2_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.Companion.NoContent
                    }
                }

                it("returns Unauthorized when missing token") {
                    testApplication {
                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        database.setTildeltEnhet(
                            ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                            enhet = UserConstants.NAV_ENHET,
                        )
                        val url = "$baseUrl/personer/single"
                        val response = client.get(url) {
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_2_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.Companion.Unauthorized
                    }
                }

                it("returns Forbidden when unauhorized application") {
                    testApplication {
                        val validTokenUnknownAzp = generateJWT(
                            audience = externalMockEnvironment.environment.azure.appClientId,
                            issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                            azp = "dev-gcp.teamsykefravr.ishuskelapp",
                        )

                        val client = setupApiAndClient()
                        personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                            personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                            isAktivVurdering = true,
                        )
                        no.nav.syfo.testutil.database.setTildeltEnhet(database)
                        val url = "$baseUrl/personer/single"
                        val response = client.get(url) {
                            bearerAuth(validTokenUnknownAzp)
                            header(NAV_PERSONIDENT_HEADER, UserConstants.ARBEIDSTAKER_FNR)
                        }
                        response.status shouldBeEqualTo HttpStatusCode.Companion.Forbidden
                    }
                }
            }
        }
    }
})
