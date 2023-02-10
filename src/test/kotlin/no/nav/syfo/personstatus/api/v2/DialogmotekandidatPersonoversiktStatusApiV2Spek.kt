package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.db.updatePersonOversiktStatusKandidat
import no.nav.syfo.personstatus.db.updatePersonOversiktStatusMotestatus
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
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

            val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)
            val mockKafkaConsumerOppfolgingstilfellePerson =
                TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson
            val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
            val kafkaOppfolgingstilfellePersonRelevant = generateKafkaOppfolgingstilfellePerson(
                personIdent = personIdentDefault,
                virksomhetsnummerList = listOf(
                    UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
                    Virksomhetsnummer(UserConstants.VIRKSOMHETSNUMMER_2),
                )
            )
            val kafkaOppfolgingstilfellePersonRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
                kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
            )
            val kafkaOppfolgingstilfellePersonService = TestKafkaModule.kafkaOppfolgingstilfellePersonService

            beforeEachTest {
                database.connection.dropData()

                clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
                every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonRecordRelevant,
                        )
                    )
                )
            }

            describe("Get dm2-kandidater for enhet") {
                val url = "$personOversiktApiV2Path/enhet/${UserConstants.NAV_ENHET}"

                it("returns NoContent, if there is a person with a tilfelle, who is kandidat, but has an open DM2 invitation") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
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

                it("returns person who is kandidat if they have a tilfelle, is kandidat, and a cancelled dm2") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
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

fun setTildeltEnhet(database: TestDatabase) {
    database.setTildeltEnhet(
        ident = PersonIdent(ARBEIDSTAKER_FNR),
        enhet = UserConstants.NAV_ENHET,
    )
}

fun setDialogmotestatus(
    database: TestDatabase,
    status: DialogmoteStatusendringType = DialogmoteStatusendringType.INNKALT
) {
    val ppersonOversiktStatus = generatePPersonOversiktStatus()
    val statusendring = DialogmoteStatusendring.create(
        generateKafkaDialogmoteStatusendring(
            personIdent = ARBEIDSTAKER_FNR,
            type = status,
            endringsTidspunkt = OffsetDateTime.now().minusDays(1)
        )
    )

    database.connection.use { connection ->
        connection.updatePersonOversiktStatusMotestatus(
            pPersonOversiktStatus = ppersonOversiktStatus,
            dialogmoteStatusendring = statusendring,
        )
        connection.commit()
    }
}

fun setAsKandidat(database: TestDatabase) {
    val ppersonOversiktStatus = generatePPersonOversiktStatus()

    database.connection.use { connection ->
        connection.updatePersonOversiktStatusKandidat(
            pPersonOversiktStatus = ppersonOversiktStatus,
            kandidat = true,
            generatedAt = OffsetDateTime.now().minusDays(10)
        )
        connection.commit()
    }
}
