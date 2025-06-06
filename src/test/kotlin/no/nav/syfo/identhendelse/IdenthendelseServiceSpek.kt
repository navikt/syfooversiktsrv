package no.nav.syfo.identhendelse

import kotlinx.coroutines.*
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
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
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val pdlClient = PdlClient(
            azureAdClient = AzureAdClient(
                azureEnvironment = externalMockEnvironment.environment.azure,
                valkeyStore = externalMockEnvironment.valkeyStore,
                httpClient = externalMockEnvironment.mockHttpClient
            ),
            clientEnvironment = externalMockEnvironment.environment.clients.pdl,
            httpClient = externalMockEnvironment.mockHttpClient
        )

        val identhendelseService = IdenthendelseService(
            database = database,
            pdlClient = pdlClient,
        )

        beforeEachTest {
            database.dropData()
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
                val oldPersonOversiktStatus = database.getPersonOversiktStatusList(oldIdent.value)
                oldPersonOversiktStatus.size shouldBeEqualTo 1

                runBlocking {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }

                // Check that person with new personident exist in db after update
                val updatedPersonOversiktStatus = database.getPersonOversiktStatusList(newIdent.value)
                updatedPersonOversiktStatus.size shouldBeEqualTo 1
                updatedPersonOversiktStatus.first().uuid shouldBeEqualTo oldPersonOversiktStatus.first().uuid
            }

            it("Skal ikke oppdatere database når person ikke finnes i databasen") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
                val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                val oldIdent = PersonIdent("12333378910")

                // Check that person with old/current personident do not exist in db before update
                val currentPersonOversiktStatus = database.getPersonOversiktStatusList(oldIdent.value)
                currentPersonOversiktStatus.size shouldBeEqualTo 0

                // Check that person with new personident do not exist in db before update
                val newPersonOversiktStatus = database.getPersonOversiktStatusList(newIdent.value)
                newPersonOversiktStatus.size shouldBeEqualTo 0

                runBlocking {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }

                // Check that person with new personident still do not exist in db after update
                val updatedPersonOversiktStatus = database.getPersonOversiktStatusList(newIdent.value)
                updatedPersonOversiktStatus.size shouldBeEqualTo 0
            }

            it("Skal ikke oppdatere database når person ikke har gamle identer") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = false)
                val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!

                // Check that person with new personident do not exist in db before update
                val newPersonOversiktStatus = database.getPersonOversiktStatusList(newIdent.value)
                newPersonOversiktStatus.size shouldBeEqualTo 0

                runBlocking {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }

                // Check that person with new personident still do not exist in db after update
                val updatedPersonOversiktStatus = database.getPersonOversiktStatusList(newIdent.value)
                updatedPersonOversiktStatus.size shouldBeEqualTo 0
            }

            it("Skal overskrive veilederident når ny ident allerede finnes i databasen") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
                val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                val veilederIdent = "Z990099"
                val personOversiktStatusWithOldIdent = PersonOversiktStatus(
                    fnr = oldIdent.value,
                ).copy(veilederIdent = veilederIdent)
                val personOversiktStatusWithNewIdent = PersonOversiktStatus(
                    fnr = newIdent.value,
                )
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(
                        commit = true,
                        personOversiktStatus = personOversiktStatusWithOldIdent,
                    )
                }
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(
                        commit = true,
                        personOversiktStatus = personOversiktStatusWithNewIdent,
                    )
                }

                runBlocking {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }

                val updatedPersonOversiktStatus = database.getPersonOversiktStatusList(newIdent.value)
                updatedPersonOversiktStatus.size shouldBeEqualTo 1
                updatedPersonOversiktStatus.first().veilederIdent shouldBeEqualTo veilederIdent

                val oldPersonOversiktStatus = database.getPersonOversiktStatusList(oldIdent.value)
                oldPersonOversiktStatus.size shouldBeEqualTo 0
            }
        }

        describe("Unhappy path") {
            it("Skal kaste feil hvis PDL ikke har oppdatert identen") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                    personident = PersonIdent(UserConstants.ARBEIDSTAKER_3_FNR),
                    hasOldPersonident = true,
                )
                val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                val newPersonOversiktStatus = PersonOversiktStatus(fnr = oldIdent.value)
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(
                        commit = true,
                        personOversiktStatus = newPersonOversiktStatus,
                    )
                }

                database.getPersonOversiktStatusList(oldIdent.value).size shouldBeEqualTo 1

                runBlocking {
                    assertFailsWith(IllegalStateException::class) {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }
                }
            }
            it("Skal kaste RuntimeException hvis PDL gir en not_found ved henting av identer") {
                val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                    personident = PersonIdent(UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR),
                    hasOldPersonident = true,
                )
                val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                val newPersonOversiktStatus = PersonOversiktStatus(fnr = oldIdent.value)
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(
                        commit = true,
                        personOversiktStatus = newPersonOversiktStatus,
                    )
                }

                database.getPersonOversiktStatusList(oldIdent.value).size shouldBeEqualTo 1

                runBlocking {
                    assertFailsWith(RuntimeException::class) {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }
                }
            }
        }
    }
})
