package no.nav.syfo.pdlpersonhendelse

import io.ktor.server.testing.*
import kotlinx.coroutines.*
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generateKafkaPersonhendelse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PersonhendelseServiceSpek : Spek({

    describe(PersonhendelseServiceSpek::class.java.simpleName) {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database

            val pdlPersonhendelseService = PdlPersonhendelseService(
                database = database,
            )

            beforeEachTest {
                database.dropData()
            }

            describe("Happy path") {
                it("Skal sette navn til NULL når person har fått nytt navn") {
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
                    updatedPersonOversiktStatus.size shouldBeEqualTo 1
                    updatedPersonOversiktStatus.first().navn shouldBeEqualTo null
                }

                it("Skal håndtere ugyldige personidenter") {
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
                    updatedPersonOversiktStatus.size shouldBeEqualTo 1
                    updatedPersonOversiktStatus.first().navn shouldNotBeEqualTo null
                }
            }
        }
    }
})
