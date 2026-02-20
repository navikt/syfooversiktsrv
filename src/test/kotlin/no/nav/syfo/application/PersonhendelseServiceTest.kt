package no.nav.syfo.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.generator.generateKafkaPersonhendelse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class PersonhendelseServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val pdlPersonhendelseService = PdlPersonhendelseService(
        database = database,
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Test
    fun `Skal sette navn til NULL når person har fått nytt navn`() {
        val kafkaPersonhendelse = generateKafkaPersonhendelse()
        val ident = kafkaPersonhendelse.personidenter.first()

        val newPersonOversiktStatus = PersonOversiktStatus(
            fnr = ident
        ).copy(
            navn = "Testnavn"
        )
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = newPersonOversiktStatus,
            )
        }

        pdlPersonhendelseService.handlePersonhendelse(kafkaPersonhendelse)

        val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ident))
        assertNotNull(updatedPersonOversiktStatus)
        assertNull(updatedPersonOversiktStatus!!.navn)
    }

    @Test
    fun `Skal håndtere ugyldige personidenter`() {
        val kafkaPersonhendelse = generateKafkaPersonhendelse()
        kafkaPersonhendelse.put("personidenter", listOf("123"))
        val ident = kafkaPersonhendelse.personidenter.first()

        val newPersonOversiktStatus = PersonOversiktStatus(
            fnr = ident
        ).copy(
            navn = "Testnavn"
        )
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = newPersonOversiktStatus,
            )
        }

        runBlocking {
            pdlPersonhendelseService.handlePersonhendelse(kafkaPersonhendelse)
        }

        assertThrows(IllegalArgumentException::class.java) {
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ident))
        }
    }
}
