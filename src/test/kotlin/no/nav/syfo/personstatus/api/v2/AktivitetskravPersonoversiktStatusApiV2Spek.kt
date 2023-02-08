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
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.time.temporal.ChronoUnit

// Samme dato som i enhetens oversikt query
private val aktivitetskravStoppunktCutoff = LocalDate.of(2023, Month.FEBRUARY, 1)

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
                    val stoppunkt = aktivitetskravStoppunktCutoff.plusDays(1)
                    persistAktivitetskrav(
                        database = database,
                        personIdent = personIdent,
                        sistVurdert = null,
                        stoppunkt = stoppunkt,
                        status = AktivitetskravStatus.NY
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
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo stoppunkt
                    }
                }

                it("returns person with aktivitetskrav status AVVENT and stoppunkt after cutoff") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val sistVurdert = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    val stoppunkt = aktivitetskravStoppunktCutoff.plusDays(1)
                    persistAktivitetskrav(
                        database = database,
                        personIdent = personIdent,
                        sistVurdert = sistVurdert,
                        stoppunkt = stoppunkt,
                        status = AktivitetskravStatus.AVVENT
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
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo stoppunkt
                    }
                }

                it("returns no content when aktivitetskrav has status AUTOMATISK_OPPFYLT") {
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val stoppunkt = aktivitetskravStoppunktCutoff.plusDays(1)
                    persistAktivitetskrav(
                        database = database,
                        personIdent = personIdent,
                        sistVurdert = null,
                        stoppunkt = stoppunkt,
                        status = AktivitetskravStatus.AUTOMATISK_OPPFYLT
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
                    val stoppunkt = aktivitetskravStoppunktCutoff.minusDays(1)
                    persistAktivitetskrav(
                        database = database,
                        personIdent = personIdent,
                        sistVurdert = null,
                        stoppunkt = stoppunkt,
                        status = AktivitetskravStatus.NY
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

fun persistAktivitetskrav(
    database: TestDatabase,
    personIdent: PersonIdent,
    sistVurdert: OffsetDateTime?,
    stoppunkt: LocalDate,
    status: AktivitetskravStatus,
) {
    val aktivitetskrav = Aktivitetskrav(
        personIdent = personIdent,
        status = status,
        sistVurdert = sistVurdert,
        stoppunkt = stoppunkt,
    )
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
