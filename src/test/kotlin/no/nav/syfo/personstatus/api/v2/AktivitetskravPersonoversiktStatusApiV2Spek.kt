package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.aktivitetskravvurdering.persistAktivitetskrav
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
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
            val kafkaOppfolgingstilfelle = TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson
            val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
            val kafkaOppfolgingstilfellePersonService = TestKafkaModule.kafkaOppfolgingstilfellePersonService
            val validToken = generateValidToken(externalMockEnvironment)
            val newTilfelleRecord = generateNewTilfelleRecord()

            beforeEachTest {
                database.connection.dropData()
                clearMocks(kafkaOppfolgingstilfelle)

                every { kafkaOppfolgingstilfelle.commitSync() } returns Unit
                every { kafkaOppfolgingstilfelle.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            newTilfelleRecord,
                        )
                    )
                )
            }

            describe("Hent personoversikt for enhet") {
                val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

                xit("includes ubehandlet aktivitetskrav from an old tilfelle, when there is a new tilfelle without aktivitetskrav") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = kafkaOppfolgingstilfelle, // Will return a new tilfelle
                    )
                    val personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
                    val updatedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    val stoppunkt = LocalDate.now().minusDays(6)
                    persistAktivitetskravFromOldTilfelle(database, personIdent, updatedAt, stoppunkt)

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
                        personOversiktStatus.aktivitetskravSistVurdert shouldBeEqualTo updatedAt.toLocalDateTimeOslo()
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo stoppunkt
                    }
                }
            }
        }
    }
})

fun generateValidToken(externalMockEnvironment: ExternalMockEnvironment): String {
    return generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = UserConstants.VEILEDER_ID,
    )
}

fun setupExternalMockEnvironment(application: Application): ExternalMockEnvironment {
    val externalMockEnvironment = ExternalMockEnvironment.instance

    application.testApiModule(
        externalMockEnvironment = externalMockEnvironment
    )
    return externalMockEnvironment
}

fun generateNewTilfelleRecord(
    endInDaysFromNow: Long = 16,
    tilfelleDuration: Long = 14
): ConsumerRecord<String, KafkaOppfolgingstilfellePerson> {
    val personIdentDefault = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)

    val kafkaOppfolgingstilfellePersonRelevant =
        generateKafkaOppfolgingstilfellePerson(
            end = LocalDate.now().plusDays(endInDaysFromNow),
            oppfolgingstilfelleDurationInDays = tilfelleDuration,
            personIdent = personIdentDefault,
            virksomhetsnummerList = listOf(
                UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
                Virksomhetsnummer(UserConstants.VIRKSOMHETSNUMMER_2),
            )
        )

    return oppfolgingstilfellePersonConsumerRecord(
        kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
    )
}

fun persistAktivitetskravFromOldTilfelle(
    database: TestDatabase,
    personIdent: PersonIdent,
    updatedAt: OffsetDateTime,
    stoppunkt: LocalDate
) {
    val aktivitetskrav = Aktivitetskrav(
        personIdent = personIdent,
        status = AktivitetskravStatus.NY,
        sistVurdert = updatedAt,
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
