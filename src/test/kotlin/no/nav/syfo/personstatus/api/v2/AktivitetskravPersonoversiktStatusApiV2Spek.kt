package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.database.*
import no.nav.syfo.testutil.generator.AktivitetskravGenerator
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

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
                database.connection.dropData()
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
                        personOversiktStatus.aktivitetskravSistVurdert shouldBeEqualTo null
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo aktivitetskrav.stoppunkt
                    }
                }

                it("returns person with aktivitetskrav status AVVENT and stoppunkt after cutoff") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val sistVurdert = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.AVVENT,
                        stoppunktAfterCutoff = true,
                        sistVurdert = sistVurdert,
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
                        personOversiktStatus.aktivitetskravSistVurdert shouldBeEqualTo sistVurdert.toLocalDateTimeOslo()
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo aktivitetskrav.stoppunkt
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
