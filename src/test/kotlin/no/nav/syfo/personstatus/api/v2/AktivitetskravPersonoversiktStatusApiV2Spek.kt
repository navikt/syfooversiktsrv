package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.database.*
import no.nav.syfo.testutil.generator.AktivitetskravGenerator
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

@InternalAPI
object AktivitetskravPersonoversiktStatusApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("Hent aktivitetskrav fra personstatusoversikt") {

        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = setupExternalMockEnvironment(application)
            val database = externalMockEnvironment.database
            val aktivitetskravGenerator =
                AktivitetskravGenerator(arenaCutoff = externalMockEnvironment.environment.arenaCutoff)
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = UserConstants.VEILEDER_ID,
            )

            beforeEachTest {
                database.dropData()
            }

            describe("Hent personoversikt for enhet") {
                val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

                it("returns person with aktivitetskrav status NY and stoppunkt after cutoff") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY,
                        stoppunktAfterCutoff = true,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = aktivitetskrav,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personIdent.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.NY.name
                        personOversiktStatus.aktivitetskravActive shouldBeEqualTo true
                    }
                }

                it("returns person with aktivitetskrav status AVVENT and stoppunkt after cutoff") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val frist = LocalDate.now().plusWeeks(1)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.AVVENT,
                        stoppunktAfterCutoff = true,
                        vurderingFrist = frist,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = aktivitetskrav,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personIdent.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.AVVENT.name
                        personOversiktStatus.aktivitetskravActive shouldBeEqualTo true
                        personOversiktStatus.aktivitetskravVurderingFrist shouldBeEqualTo frist
                    }
                }

                it("returns person with aktivitetskrav status NY_VURDERING") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY_VURDERING,
                        stoppunktAfterCutoff = true,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = aktivitetskrav,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personIdent.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.NY_VURDERING.name
                        personOversiktStatus.aktivitetskravActive shouldBeEqualTo true
                    }
                }

                it("returns no content when aktivitetskrav has status LUKKET") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val nyttAtivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY,
                        stoppunktAfterCutoff = true,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = nyttAtivitetskrav,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.NY.name
                    }

                    val lukketAktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.LUKKET,
                        stoppunktAfterCutoff = true,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = lukketAktivitetskrav,
                    )
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("returns no content when aktivitetskrav has status AUTOMATISK_OPPFYLT") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.AUTOMATISK_OPPFYLT,
                        stoppunktAfterCutoff = true,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = aktivitetskrav,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
                it("returns no content when person with aktivitetskrav status NY and stoppunkt before cutoff") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY,
                        stoppunktAfterCutoff = false,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = aktivitetskrav,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
                it("returns no content when no aktivitetskrav and the person is removed as kandidat in the application level filter") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY,
                        stoppunktAfterCutoff = false,
                    )
                    persistAktivitetskravAndTildelEnhet(
                        database = database,
                        personIdent = personIdent,
                        aktivitetskrav = aktivitetskrav,
                    )
                    setAsKandidat(database)
                    setAsOpenDialogmote(database, personIdent) // remove person as kandidat

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
            }
        }
    }
})
