package no.nav.syfo.personstatus.domain

import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.aktivitetskravvurdering.domain.toPersonOversiktStatus
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.AktivitetskravGenerator
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

val arenaCutoff: LocalDate = LocalDate.now()

class PersonOversikstStatusSpek : Spek({
    val aktivitetskravGenerator = AktivitetskravGenerator(
        arenaCutoff = arenaCutoff,
    )
    val defaultPersonident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)

    describe("isActiveAktivitetskrav") {
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
    }
})
