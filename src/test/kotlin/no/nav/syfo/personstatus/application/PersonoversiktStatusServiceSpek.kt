package no.nav.syfo.personstatus.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonoversiktStatusServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    @Nested
    inner class UpdateNavnOrFodselsdatoWhereMissing {
        @Test
        fun `correctly updates navn and or fodselsdato for persons`() {
            database.dropData()
            val personMissingNavnAndFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_FNR,
                    isAktivAktivitetskravvurdering = true,
                )
            )
            val personMissingNavnAndFodselsdatoNotRelevant =
                personOversiktStatusRepository.createPersonOversiktStatus(PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_2_FNR))
            val personMissingNavn = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_3_FNR,
                    fodselsdato = LocalDate.now(),
                    isAktivAktivitetskravvurdering = true,
                )
            )
            val personMissingFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_4_FNR,
                    navn = "Sylvia Sykmeldt",
                    isAktivAktivitetskravvurdering = true,
                )
            )
            val personNotMissingButActive = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_5_FNR,
                    navn = "Sylvia Sykmeldt",
                    fodselsdato = LocalDate.now(),
                    isAktivAktivitetskravvurdering = true,
                )
            )

            runBlocking {
                personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(updateLimit = 10)
            }

            val personMissingNavnAndFodselsdatoEdited =
                personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
            val personMissingNavnAndFodselsdatoNotRelevantEdited =
                personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_2_FNR))
            val personMissingNavnEdited =
                personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_3_FNR))
            val personMissingFodselsdatoEdited =
                personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_4_FNR))
            val personNotMissingButActiveEdited =
                personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_5_FNR))

            personMissingNavnAndFodselsdato.fnr shouldBeEqualTo personMissingNavnAndFodselsdatoEdited?.fnr
            personMissingNavnAndFodselsdatoEdited?.navn shouldNotBeEqualTo null
            personMissingNavnAndFodselsdatoEdited?.fodselsdato shouldNotBeEqualTo null

            personMissingNavnAndFodselsdatoNotRelevant.fnr shouldBeEqualTo personMissingNavnAndFodselsdatoNotRelevantEdited?.fnr
            personMissingNavnAndFodselsdatoNotRelevantEdited?.navn shouldBeEqualTo personMissingNavnAndFodselsdatoNotRelevant.navn
            personMissingNavnAndFodselsdatoNotRelevantEdited?.fodselsdato shouldBeEqualTo personMissingNavnAndFodselsdatoNotRelevant.fodselsdato

            personMissingNavn.fnr shouldBeEqualTo personMissingNavnEdited?.fnr
            personMissingNavn.navn shouldBeEqualTo null
            personMissingNavnEdited?.navn shouldNotBeEqualTo null

            personMissingFodselsdato.fnr shouldBeEqualTo personMissingFodselsdatoEdited?.fnr
            personMissingFodselsdato.fodselsdato shouldBeEqualTo null
            personMissingFodselsdatoEdited?.fodselsdato shouldNotBeEqualTo null

            personNotMissingButActive.fnr shouldBeEqualTo personNotMissingButActiveEdited?.fnr
            personNotMissingButActive.navn shouldBeEqualTo personNotMissingButActiveEdited?.navn
            personNotMissingButActive.fodselsdato shouldBeEqualTo personNotMissingButActiveEdited?.fodselsdato
        }

        @Test
        fun `correctly updates navn for person missing name and fodselsdato and missing fodselsdato in PDL`() {
            database.dropData()
            val personMissingNavnAndFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_NO_FODSELSDATO,
                    isAktivAktivitetskravvurdering = true,
                )
            )

            runBlocking {
                personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(updateLimit = 10)
            }

            val personMissingNavnAndFodselsdatoEdited =
                personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_NO_FODSELSDATO))

            personMissingNavnAndFodselsdato.fnr shouldBeEqualTo personMissingNavnAndFodselsdatoEdited?.fnr
            personMissingNavnAndFodselsdatoEdited?.navn.shouldNotBeNull()
            personMissingNavnAndFodselsdatoEdited?.fodselsdato.shouldBeNull()
        }
    }
}
