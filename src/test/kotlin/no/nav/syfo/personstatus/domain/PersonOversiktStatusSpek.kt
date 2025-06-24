package no.nav.syfo.personstatus.domain

import no.nav.syfo.dialogmotekandidat.kafka.toPersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringStoppunkt
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class PersonOversiktStatusTest {
    private val defaultPersonident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)

    @Nested
    inner class IsDialogmotekandidat {
        private val activeOppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusWeeks(2),
            end = LocalDate.now().plusDays(5),
            antallSykedager = 19
        )

        @Test
        fun `returns true if dialogmotekandidat generated after start of latest oppfolgingstilfelle and more than seven days ago`() {
            val dialogmotekandidatEndring = generateKafkaDialogmotekandidatEndringStoppunkt(
                personIdent = defaultPersonident.value,
                createdAt = OffsetDateTime.now().minusDays(8),
            )
            val personOversiktStatus = dialogmotekandidatEndring.toPersonOversiktStatus()
                .copy(
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle
                )
            personOversiktStatus.isDialogmotekandidat() shouldBeEqualTo true
        }

        @Test
        fun `returns true if dialogmotekandidat generated after start of latest oppfolgingstilfelle and seven days ago`() {
            val dialogmotekandidatEndring = generateKafkaDialogmotekandidatEndringStoppunkt(
                personIdent = defaultPersonident.value,
                createdAt = OffsetDateTime.now().minusDays(7),
            )
            val personOversiktStatus = dialogmotekandidatEndring.toPersonOversiktStatus()
                .copy(
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle
                )
            personOversiktStatus.isDialogmotekandidat() shouldBeEqualTo true
        }

        @Test
        fun `returns false if dialogmotekandidat generated after start of latest oppfolgingstilfelle but less than seven days ago`() {
            val dialogmotekandidatEndring = generateKafkaDialogmotekandidatEndringStoppunkt(
                personIdent = defaultPersonident.value,
                createdAt = OffsetDateTime.now().minusDays(6),
            )
            val personOversiktStatus = dialogmotekandidatEndring.toPersonOversiktStatus()
                .copy(
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle
                )
            personOversiktStatus.isDialogmotekandidat() shouldBeEqualTo false
        }
    }
}
