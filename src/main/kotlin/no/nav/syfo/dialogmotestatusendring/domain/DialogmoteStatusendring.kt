package no.nav.syfo.dialogmotestatusendring.domain

import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset

enum class DialogmoteStatusendringType {
    INNKALT,
    AVLYST,
    FERDIGSTILT,
    NYTT_TID_STED,
    LUKKET,
}

data class DialogmoteStatusendring private constructor(
    val personIdent: PersonIdent,
    val type: DialogmoteStatusendringType,
    val endringTidspunkt: OffsetDateTime,
) {

    companion object {
        fun create(kafkaDialogmoteStatusEndring: KDialogmoteStatusEndring) = DialogmoteStatusendring(
            personIdent = PersonIdent(kafkaDialogmoteStatusEndring.getPersonIdent()),
            type = DialogmoteStatusendringType.valueOf(kafkaDialogmoteStatusEndring.getStatusEndringType()),
            endringTidspunkt = OffsetDateTime.ofInstant(
                kafkaDialogmoteStatusEndring.getStatusEndringTidspunkt(),
                ZoneOffset.UTC
            )
        )
    }

    fun toPersonOversiktStatus(): PersonOversiktStatus = PersonOversiktStatus(
        veilederIdent = null,
        fnr = this.personIdent.value,
        navn = null,
        enhet = null,
        motebehovUbehandlet = null,
        oppfolgingsplanLPSBistandUbehandlet = null,
        dialogmotesvarUbehandlet = false,
        dialogmotekandidat = null,
        dialogmotekandidatGeneratedAt = null,
        motestatus = type.name,
        motestatusGeneratedAt = endringTidspunkt,
        latestOppfolgingstilfelle = null,
        aktivitetskrav = null,
        aktivitetskravStoppunkt = null,
        aktivitetskravSistVurdert = null,
        aktivitetskravVurderingFrist = null,
        behandlerdialogSvarUbehandlet = false,
        behandlerdialogUbesvartUbehandlet = false,
    )
}
