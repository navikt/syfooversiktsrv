package no.nav.syfo.personstatus.domain

import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat.toPersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringStoppunkt
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class PersonOversiktStatusTest {
    private val defaultPersonident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
    private val activeOppfolgingstilfelle = generateOppfolgingstilfelle(
        start = LocalDate.now().minusWeeks(2),
        end = LocalDate.now().plusDays(5),
        antallSykedager = 19
    )

    /*
     * Vi lar det gå syv dager fra personen er generert som dialogmøtekandidat til de dukker opp i oversikten slik at partene rekker å svare på møtebehov.
     */
    @Test
    fun `Returns true if dialogmotekandidat generated after start of latest oppfolgingstilfelle and more than seven days ago`() {
        val dialogmotekandidatEndring = generateKafkaDialogmotekandidatEndringStoppunkt(
            personIdent = defaultPersonident.value,
            createdAt = OffsetDateTime.now().minusDays(8),
        )
        val personOversiktStatus = dialogmotekandidatEndring.toPersonOversiktStatus()
            .copy(
                latestOppfolgingstilfelle = activeOppfolgingstilfelle
            )
        assertTrue(personOversiktStatus.isDialogmotekandidat())
    }

    @Test
    fun `Returns true if dialogmotekandidat generated after start of latest oppfolgingstilfelle and seven days ago`() {
        val dialogmotekandidatEndring = generateKafkaDialogmotekandidatEndringStoppunkt(
            personIdent = defaultPersonident.value,
            createdAt = OffsetDateTime.now().minusDays(7),
        )
        val personOversiktStatus = dialogmotekandidatEndring.toPersonOversiktStatus()
            .copy(
                latestOppfolgingstilfelle = activeOppfolgingstilfelle
            )
        assertTrue(personOversiktStatus.isDialogmotekandidat())
    }

    @Test
    fun `Returns false if dialogmotekandidat generated after start of latest oppfolgingstilfelle but less than seven days ago`() {
        val dialogmotekandidatEndring = generateKafkaDialogmotekandidatEndringStoppunkt(
            personIdent = defaultPersonident.value,
            createdAt = OffsetDateTime.now().minusDays(6),
        )
        val personOversiktStatus = dialogmotekandidatEndring.toPersonOversiktStatus()
            .copy(
                latestOppfolgingstilfelle = activeOppfolgingstilfelle
            )
        assertFalse(personOversiktStatus.isDialogmotekandidat())
    }
}
