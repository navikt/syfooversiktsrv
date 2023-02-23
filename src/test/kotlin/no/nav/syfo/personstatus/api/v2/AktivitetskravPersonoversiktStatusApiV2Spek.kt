package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.aktivitetskravvurdering.persistAktivitetskrav
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generateAktivitetskrav
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.time.temporal.ChronoUnit

@InternalAPI
object AktivitetskravPersonoversiktStatusApiV2 : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("Hent aktivitetskrav fra personstatusoversikt") {

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

            describe("Hent personoversikt for enhet") {
                val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

                it("returns person with aktivitetskrav status NY and stoppunkt after cutoff") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val aktivitetskrav = generateAktivitetskrav(
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
                    val aktivitetskrav = generateAktivitetskrav(
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
                    val aktivitetskrav = generateAktivitetskrav(
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
                    val aktivitetskrav = generateAktivitetskrav(
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
            }
        }
    }
})

fun setupExternalMockEnvironment(application: Application): ExternalMockEnvironment {
    val externalMockEnvironment = ExternalMockEnvironment.instance

    application.testApiModule(
        externalMockEnvironment = externalMockEnvironment
    )
    return externalMockEnvironment
}

fun persistAktivitetskravAndTildelEnhet(
    database: TestDatabase,
    personIdent: PersonIdent,
    aktivitetskrav: Aktivitetskrav,
) {
    database.connection.use { connection ->
        persistAktivitetskrav(
            connection = connection,
            aktivitetskrav = aktivitetskrav,
        )
        connection.commit()
    }
    database.setTildeltEnhet(
        ident = personIdent,
        enhet = UserConstants.NAV_ENHET,
    )
}
