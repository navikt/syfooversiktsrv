package no.nav.syfo.personstatus.infrastructure

import io.ktor.server.testing.*
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.PersonOversiktStatusRepository
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotBeEmpty
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PersonOversiktStatusRepositorySpek : Spek({

    describe(PersonOversiktStatusRepository::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)

            beforeEachTest { database.dropData() }
            afterEachGroup { database.dropData() }

            describe("updateArbeidsuforhetVurderingStatus") {
                it("Successfully updates arbeidsuforhet vurdering status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.updateArbeidsuforhetVurderingStatus(
                        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true
                    )

                    result.isSuccess shouldBe true
                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    newPersonOversiktStatus.isAktivArbeidsuforhetVurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetVurdering
                    personOversiktStatus.isAktivArbeidsuforhetVurdering shouldBe true
                }

                it("Successfully updates arbeidsuforhet vurdering status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivArbeidsuforhetVurdering = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.updateArbeidsuforhetVurderingStatus(
                        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false
                    )

                    result.isSuccess shouldBe true
                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    newPersonOversiktStatus.isAktivArbeidsuforhetVurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetVurdering
                    personOversiktStatus.isAktivArbeidsuforhetVurdering shouldBe false
                }

                it("Creates new person when none exist when updating arbeidsuforhet vurdering status") {
                    val result = personOversiktStatusRepository.updateArbeidsuforhetVurderingStatus(
                        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false
                    )

                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    personOversiktStatus.shouldNotBeEmpty()
                    result.isSuccess shouldBe true
                }
            }

            describe("getPersonOversiktStatus") {
                it("Retrieves person oversikt status when one exists") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val personStatus =
                        personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
                    personStatus shouldNotBe null
                    personStatus?.fnr shouldBeEqualTo newPersonOversiktStatus.fnr
                }
                it("Handles trying to retrieve non existing person oversikt status") {
                    val personStatus =
                        personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))

                    personStatus shouldBe null
                }
            }
        }
    }
})
