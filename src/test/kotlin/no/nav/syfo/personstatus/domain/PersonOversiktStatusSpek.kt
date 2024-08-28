package no.nav.syfo.personstatus.domain

import no.nav.syfo.dialogmotekandidat.kafka.toPersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringStoppunkt
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime

class PersonOversiktStatusSpek : Spek({
    val defaultPersonident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)

    describe("isDialogmotekandidat") {
        val activeOppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusWeeks(2),
            end = LocalDate.now().plusDays(5),
            antallSykedager = 19
        )

        /*
         * Vi lar det gå syv dager fra personen er generert som dialogmøtekandidat til de dukker opp i oversikten slik at partene rekker å svare på møtebehov.
         */
        it("returns true if dialogmotekandidat generated after start of latest oppfolgingstilfelle and more than seven days ago") {
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
        it("returns true if dialogmotekandidat generated after start of latest oppfolgingstilfelle and seven days ago") {
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
        it("returns false if dialogmotekandidat generated after start of latest oppfolgingstilfelle but less than seven days ago") {
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
})
