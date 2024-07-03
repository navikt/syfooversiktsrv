package no.nav.syfo.personstatus.domain

import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.aktivitetskravvurdering.domain.toPersonOversiktStatus
import no.nav.syfo.dialogmotekandidat.kafka.toPersonOversiktStatus
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.AktivitetskravGenerator
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringStoppunkt
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime

val arenaCutoff: LocalDate = LocalDate.now()

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

    describe("isActiveAktivitetskrav") {
        val aktivitetskravGenerator = AktivitetskravGenerator(
            arenaCutoff = arenaCutoff,
        )
        it("returns true if AktivitetskravStatus is NY and stoppunkt after arena cutoff") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.NY,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo true
        }

        it("return true if AktivitetskravStatus is AVVENT and stoppunkt after arena cutoff") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.AVVENT,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo true
        }
        it("return false if AktivitetskravStatus is NY and stoppunkt equal arena cutoff") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.NY,
                stoppunktAfterCutoff = false,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return false if AktivitetskravStatus is AVVENT and stoppunkt before arena cutoff") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.AVVENT,
                stoppunktAfterCutoff = false,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return false if AktivitetskravStatus is OPPFYLT") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.OPPFYLT,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return false if AktivitetskravStatus is AUTOMATISK_OPPFYLT") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.AUTOMATISK_OPPFYLT,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return false if AktivitetskravStatus is STANS") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.STANS,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return false if AktivitetskravStatus is UNNTAK") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.UNNTAK,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return false if AktivitetskravStatus is IKKE_OPPFYLT") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.IKKE_OPPFYLT,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo false
        }

        it("return true if AktivitetskravStatus is NY_VURDERING") {
            val person = aktivitetskravGenerator.generateAktivitetskrav(
                personIdent = defaultPersonident,
                status = AktivitetskravStatus.NY_VURDERING,
                stoppunktAfterCutoff = true,
            ).toPersonOversiktStatus()

            person.isActiveAktivitetskrav(arenaCutoff) shouldBeEqualTo true
        }
    }
})
