package no.nav.syfo.personstatus.infrastructure.database

import io.ktor.server.testing.*
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.*
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

            describe("updateArbeidsuforhetvurderingStatus") {
                it("Successfully updates arbeidsuforhet vurdering status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true
                    )

                    result.isSuccess shouldBe true
                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    newPersonOversiktStatus.isAktivArbeidsuforhetvurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetvurdering
                    personOversiktStatus.isAktivArbeidsuforhetvurdering shouldBe true
                }

                it("Successfully updates arbeidsuforhet vurdering status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivArbeidsuforhetvurdering = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false
                    )

                    result.isSuccess shouldBe true
                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    newPersonOversiktStatus.isAktivArbeidsuforhetvurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetvurdering
                    personOversiktStatus.isAktivArbeidsuforhetvurdering shouldBe false
                }

                it("Creates new person when none exist when updating arbeidsuforhet vurdering status") {
                    val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false
                    )

                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    personOversiktStatus.shouldNotBeEmpty()
                    result.isSuccess shouldBe true
                }
            }

            describe("upsertSenOppfolgingKandidatStatus") {
                it("Successfully updates mer oppfolging kandidat status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivKandidat = true,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldNotBeEqualTo newPersonOversiktStatus.isAktivSenOppfolgingKandidat
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldBe true
                }

                it("Successfully updates mer oppfolging kandidat status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivSenOppfolgingKandidat = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivKandidat = false,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldNotBeEqualTo newPersonOversiktStatus.isAktivSenOppfolgingKandidat
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldBe false
                }

                it("Creates new person when none exist when upserting mer oppfolging kandidat status") {
                    val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivKandidat = true,
                    )

                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    pPersonOversiktStatus.shouldNotBeEmpty()
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

            describe("Update aktivitetskrav aktiv vurdering") {
                it("Successfully updates aktiv aktivitetskrav status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldNotBeEqualTo newPersonOversiktStatus.isAktivAktivitetskravvurdering
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldBe true
                }

                it("Successfully updates aktivitetskrav aktiv status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivAktivitetskravvurdering = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldNotBeEqualTo newPersonOversiktStatus.isAktivAktivitetskravvurdering
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldBe false
                }

                it("Creates new person when none exist when upserting mer oppfolging kandidat status") {
                    val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )

                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    pPersonOversiktStatus.shouldNotBeEmpty()
                    result.isSuccess shouldBe true
                }
            }
        }
    }
})
