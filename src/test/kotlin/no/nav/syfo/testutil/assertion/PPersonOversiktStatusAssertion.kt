package no.nav.syfo.testutil.assertion

import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull

fun checkPPersonOversiktStatus(
    pPersonOversiktStatus: PPersonOversiktStatus,
    oversikthendelsetilfelle: KOversikthendelsetilfelle,
    veilederIdent: String?,
) {
    pPersonOversiktStatus.fnr shouldBeEqualTo oversikthendelsetilfelle.fnr
    pPersonOversiktStatus.navn shouldBeEqualTo null
    pPersonOversiktStatus.veilederIdent shouldBeEqualTo veilederIdent
    pPersonOversiktStatus.enhet shouldBeEqualTo oversikthendelsetilfelle.enhetId
    pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
    pPersonOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
    pPersonOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
}

fun checkPPersonOversiktStatusOppfolgingstilfelle(
    pPersonOversiktStatus: PPersonOversiktStatus,
    kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
) {
    val latestOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.firstOrNull()

    latestOppfolgingstilfelle.shouldNotBeNull()

    pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.toInstant()?.toEpochMilli() shouldBeEqualTo kafkaOppfolgingstilfellePerson.createdAt.toInstant().toEpochMilli()
    pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.offset shouldBeEqualTo kafkaOppfolgingstilfellePerson.createdAt.offset
    pPersonOversiktStatus.oppfolgingstilfelleUpdatedAt.shouldNotBeNull()
    pPersonOversiktStatus.oppfolgingstilfelleStart shouldBeEqualTo latestOppfolgingstilfelle.start
    pPersonOversiktStatus.oppfolgingstilfelleEnd shouldBeEqualTo latestOppfolgingstilfelle.end
    pPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.toInstant()?.toEpochMilli() shouldBeEqualTo kafkaOppfolgingstilfellePerson.referanseTilfelleBitInntruffet.toInstant().toEpochMilli()
    pPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.offset shouldBeEqualTo kafkaOppfolgingstilfellePerson.referanseTilfelleBitInntruffet.offset
    pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.toString() shouldBeEqualTo kafkaOppfolgingstilfellePerson.referanseTilfelleBitUuid
}
