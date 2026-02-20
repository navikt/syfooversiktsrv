package no.nav.syfo.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.generateKafkaIdenthendelseDTO
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

class IdenthendelseServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val pdlClient = PdlClient(
        azureAdClient = AzureAdClient(
            azureEnvironment = externalMockEnvironment.environment.azure,
            valkeyStore = externalMockEnvironment.valkeyStore,
            httpClient = externalMockEnvironment.mockHttpClient
        ),
        clientEnvironment = externalMockEnvironment.environment.clients.pdl,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    private val identhendelseService = IdenthendelseService(
        database = database,
        pdlClient = pdlClient,
        personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository,
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {

        @Test
        fun `Skal oppdatere database når person har fått ny ident`() {
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
            val oldPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(oldIdent)
            assertNotNull(oldPersonOversiktStatus)

            runBlocking {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }

            // Check that person with new personident exist in db after update
            val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(newIdent)
            assertNotNull(updatedPersonOversiktStatus)
            assertEquals(oldPersonOversiktStatus!!.uuid, updatedPersonOversiktStatus!!.uuid)
        }

        @Test
        fun `Skal ikke oppdatere database når person ikke finnes i databasen`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
            val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
            val oldIdent = PersonIdent("12333378910")

            // Check that person with old/current personident do not exist in db before update
            val currentPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(oldIdent)
            assertNull(currentPersonOversiktStatus)

            // Check that person with new personident do not exist in db before update
            val newPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(newIdent)
            assertNull(newPersonOversiktStatus)

            runBlocking {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }

            // Check that person with new personident still do not exist in db after update
            val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(newIdent)
            assertNull(updatedPersonOversiktStatus)
        }

        @Test
        fun `Skal ikke oppdatere database når person ikke har gamle identer`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = false)
            val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!

            // Check that person with new personident do not exist in db before update
            val newPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(newIdent)
            assertNull(newPersonOversiktStatus)

            runBlocking {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }

            // Check that person with new personident still do not exist in db after update
            val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(newIdent)
            assertNull(updatedPersonOversiktStatus)
        }

        @Test
        fun `Skal overskrive veilederident når ny ident allerede finnes i databasen`() {
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

            val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(newIdent)
            assertNotNull(updatedPersonOversiktStatus)
            assertEquals(veilederIdent, updatedPersonOversiktStatus!!.veilederIdent)

            val oldPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(oldIdent)
            assertNull(oldPersonOversiktStatus)
        }
    }

    @Nested
    @DisplayName("Unhappy path")
    inner class UnhappyPath {

        @Test
        fun `Skal kaste feil hvis PDL ikke har oppdatert identen`() {
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

            assertNotNull(personOversiktStatusRepository.getPersonOversiktStatus(oldIdent))

            runBlocking {
                assertThrows(IllegalStateException::class.java) {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }
            }
        }

        @Test
        fun `Skal kaste RuntimeException hvis PDL gir en not_found ved henting av identer`() {
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

            assertNotNull(personOversiktStatusRepository.getPersonOversiktStatus(oldIdent))

            runBlocking {
                assertThrows(RuntimeException::class.java) {
                    identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                }
            }
        }
    }
}
