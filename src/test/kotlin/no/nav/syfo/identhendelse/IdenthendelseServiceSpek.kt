package no.nav.syfo.identhendelse

import io.ktor.server.testing.*
import kotlinx.coroutines.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generateKafkaIdenthendelseDTO
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object IdenthendelseServiceSpek : Spek({

    describe(IdenthendelseServiceSpek::class.java.simpleName) {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val pdlClient = PdlClient(
                azureAdClient = AzureAdClient(
                    azureEnvironment = externalMockEnvironment.environment.azure,
                    redisStore = RedisStore(externalMockEnvironment.environment.redis),
                ),
                clientEnvironment = externalMockEnvironment.environment.clients.pdl,
                redisStore = RedisStore(externalMockEnvironment.environment.redis),
            )

            val identhendelseService = IdenthendelseService(
                database = database,
                pdlClient = pdlClient,
            )

            beforeEachTest {
                database.connection.dropData()
            }

            describe("Happy path") {
                it("Skal oppdatere database når person har fått ny ident") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
                    val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                    val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                    // Populate database with new PersonOversiktStatus using old ident for person
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = oldIdent.value)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    // Check that person with old/current personident exist in db before update
                    val currentMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(oldIdent.value)
                    currentMotedeltakerArbeidstaker.size shouldBeEqualTo 1

                    runBlocking {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }

                    // Check that person with new personident exist in db after update
                    val updatedMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(newIdent.value)
                    updatedMotedeltakerArbeidstaker.size shouldBeEqualTo 1

                    // Check that person with old personident do not exist in db after update
                    val oldMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(oldIdent.value)
                    oldMotedeltakerArbeidstaker.size shouldBeEqualTo 0
                }

                it("Skal ikke oppdatere database når person ikke finnes i databasen") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
                    val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                    val oldIdent = PersonIdent("12333378910")

                    // Check that person with old/current personident do not exist in db before update
                    val currentMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(oldIdent.value)
                    currentMotedeltakerArbeidstaker.size shouldBeEqualTo 0

                    // Check that person with new personident do not exist in db before update
                    val newMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(newIdent.value)
                    newMotedeltakerArbeidstaker.size shouldBeEqualTo 0

                    runBlocking {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }

                    // Check that person with new personident still do not exist in db after update
                    val updatedMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(newIdent.value)
                    updatedMotedeltakerArbeidstaker.size shouldBeEqualTo 0
                }

                it("Skal ikke oppdatere database når person ikke har gamle identer") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = false)
                    val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!

                    // Check that person with new personident do not exist in db before update
                    val newMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(newIdent.value)
                    newMotedeltakerArbeidstaker.size shouldBeEqualTo 0

                    runBlocking {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }

                    // Check that person with new personident still do not exist in db after update
                    val updatedMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(newIdent.value)
                    updatedMotedeltakerArbeidstaker.size shouldBeEqualTo 0
                }
            }

            describe("Unhappy path") {
                it("Skal kaste feil hvis PDL ikke har oppdatert identen") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_3_FNR),
                        hasOldPersonident = true,
                    )
                    val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                    // Populate database with new PersonOversiktStatus using old ident for person
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = oldIdent.value)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    // Check that person with old/current personident exist in db before update
                    val currentMotedeltakerArbeidstaker = database.getPersonOversiktStatusList(oldIdent.value)
                    currentMotedeltakerArbeidstaker.size shouldBeEqualTo 1

                    runBlocking {
                        assertFailsWith(IllegalStateException::class) {
                            identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                        }
                    }
                }
            }
        }
    }
})
