package no.nav.syfo.pdlpersonhendelse

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.application.PdlPersonhendelseService
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generateKafkaPersonhendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class PersonhendelseServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val pdlPersonhendelseService = PdlPersonhendelseService(
        database = database,
    )

    @BeforeEach
    fun setUp() {
        database.dropData()
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

        runBlocking {
            pdlPersonhendelseService.handlePersonhendelse(kafkaPersonhendelse)
        }

        val updatedPersonOversiktStatus = database.getPersonOversiktStatusList(ident)
        assertEquals(1, updatedPersonOversiktStatus.size)
        assertNull(updatedPersonOversiktStatus.first().navn)
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

        val updatedPersonOversiktStatus = database.getPersonOversiktStatusList(ident)
        assertEquals(1, updatedPersonOversiktStatus.size)
        assertNotNull(updatedPersonOversiktStatus.first().navn)
    }
}
