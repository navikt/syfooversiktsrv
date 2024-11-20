package no.nav.syfo.cronjob.behandlendeenhet

import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
object PersonBehandlendeEnhetCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
        val internalMockEnvironment = InternalMockEnvironment.instance

        val personBehandlendeEnhetCronjob = internalMockEnvironment.personBehandlendeEnhetCronjob

        val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

        beforeEachTest {
            database.dropData()
        }

        describe(PersonBehandlendeEnhetCronjobSpek::class.java.simpleName) {

            describe("Successful processing") {
                it("should not update Enhet of existing PersonOversiktStatus with no ubehandlet oppgave") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = ARBEIDSTAKER_FNR
                        )
                    )

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = ARBEIDSTAKER_FNR,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update Enhet and remove Veileder of existing PersonOversiktStatus with other Enhet and ubehandlet oppgave") {
                    val firstEnhet = NAV_ENHET_2
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = ARBEIDSTAKER_FNR,
                            motebehovUbehandlet = true,
                            enhet = firstEnhet,
                            veilederIdent = UserConstants.VEILEDER_ID,
                        )
                    )
                    val tildeltEnhetUpdatedAtBeforeUpdate = nowUTC().minusDays(2)
                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdentDefault,
                        time = tildeltEnhetUpdatedAtBeforeUpdate,
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdentDefault.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.enhet shouldNotBeEqualTo firstEnhet
                        pPersonOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt!!.toInstant()
                            .toEpochMilli() shouldBeGreaterThan tildeltEnhetUpdatedAtBeforeUpdate.toInstant()
                            .toEpochMilli()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update Enhet if dialogmotekandidat is true") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = ARBEIDSTAKER_FNR,
                            dialogmotekandidat = true,
                        )
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    val pPersonOversiktStatusList = database.getPersonOversiktStatusList(fnr = personIdentDefault.value)
                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.enhet.shouldNotBeNull()
                    pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update Enhet and remove Veileder of existing PersonOversiktStatus with no Enhet and ubehandlet oppgave") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = ARBEIDSTAKER_FNR,
                            oppfolgingsplanLPSBistandUbehandlet = true,
                            veilederIdent = UserConstants.VEILEDER_ID,
                        )
                    )

                    val tildeltEnhetUpdatedAtBeforeUpdate = nowUTC().minusDays(2)

                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdentDefault,
                        time = tildeltEnhetUpdatedAtBeforeUpdate,
                    )
                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdentDefault.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt shouldNotBeEqualTo tildeltEnhetUpdatedAtBeforeUpdate
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update tildeltEnhetUpdatedAt, but not tildeltEnhet, of existing PersonOversiktStatus with ubehandlet oppgave if BehandlendeEnhet is not found") {
                    val personIdent = ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = personIdent.value,
                            oppfolgingsplanLPSBistandUbehandlet = true,
                        )
                    )

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdent.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    val tildeltEnhetUpdatedAtBeforeUpdate = nowUTC().minusDays(2)
                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdent,
                        time = tildeltEnhetUpdatedAtBeforeUpdate,
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdent.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt!!.toInstant()
                            .toEpochMilli() shouldBeGreaterThan tildeltEnhetUpdatedAtBeforeUpdate!!.toInstant()
                            .toEpochMilli()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("don't update if ubehandlet oppgave but enhet updated less than 24 hours ago") {
                    val firstEnhet = NAV_ENHET_2
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = ARBEIDSTAKER_FNR,
                            motebehovUbehandlet = true,
                            enhet = firstEnhet,
                        )
                    )
                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdentDefault,
                        time = nowUTC().minusHours(22),
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }
            }

            describe("Unsuccessful processing") {
                it("should fail to update Enhet of existing PersonOversiktStatus exception is thrown when requesting Enhet from Syfobehandlendeenhet") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value,
                            oppfolgingsplanLPSBistandUbehandlet = true,
                        )
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 1
                        result.updated shouldBeEqualTo 0
                    }
                }
            }
        }
    }
})
