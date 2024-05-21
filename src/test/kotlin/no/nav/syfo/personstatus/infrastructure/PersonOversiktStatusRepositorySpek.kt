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
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class PersonOversiktStatusRepositorySpek : Spek({

    describe(PersonOversiktStatusRepository::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)

            afterEachTest {
                database.connection.dropData()
            }

            describe("Successfully updates arbeidsuforhet vurdering status") {
                val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(
                        commit = true,
                        personOversiktStatus = newPersonOversiktStatus,
                    )
                }

                val rowsUpdated = personOversiktStatusRepository.updateArbeidsuforhetVurderingStatus(
                    personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                    isAktivVurdering = true
                )

                rowsUpdated shouldBe 1
                val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                newPersonOversiktStatus.isAktivArbeidsuforhetVurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetVurdering
                personOversiktStatus.isAktivArbeidsuforhetVurdering shouldBe true
            }
        }
    }
})
