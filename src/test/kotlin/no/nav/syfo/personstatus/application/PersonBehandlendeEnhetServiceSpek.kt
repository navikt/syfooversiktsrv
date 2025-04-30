package no.nav.syfo.personstatus.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import org.amshove.kluent.*

class PersonBehandlendeEnhetServiceSpek : Spek({

    describe(PersonBehandlendeEnhetService::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val personBehandlendeEnhetService = externalMockEnvironment.personBehandlendeEnhetService
        val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

        beforeEachTest { database.dropData() }
        afterEachGroup { database.dropData() }

        describe("updateBehandlendeEnhet") {
            it("correctly updates enhet when no enhet assigned") {
                personOversiktStatusRepository.createPersonOversiktStatus(
                    PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR, enhet = null)
                )

                runBlocking {
                    personBehandlendeEnhetService.updateBehandlendeEnhet(personIdent = PersonIdent(ARBEIDSTAKER_FNR))
                }

                val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))
                personOversiktStatus shouldNotBe null
                personOversiktStatus!!.enhet shouldBeEqualTo behandlendeEnhetDTO.oppfolgingsenhet.enhetId
            }

            it("correctly updates enhet when other enhet is already assigned") {
                personOversiktStatusRepository.createPersonOversiktStatus(
                    PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR, enhet = "0314")
                )

                runBlocking {
                    personBehandlendeEnhetService.updateBehandlendeEnhet(personIdent = PersonIdent(ARBEIDSTAKER_FNR))
                }

                val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))
                personOversiktStatus shouldNotBe null
                personOversiktStatus!!.enhet shouldBeEqualTo behandlendeEnhetDTO.oppfolgingsenhet.enhetId
            }
        }
    }
})
