package no.nav.syfo.personstatus.application.aktivitetskrav

import java.time.LocalDate
import java.time.LocalDateTime

data class AktivitetskravRequestDTO(
    val personidenter: List<String>,
)

data class GetAktivitetskravForPersonsResponseDTO(
    val aktivitetskravvurderinger: Map<String, AktivitetskravDTO>,
)

data class AktivitetskravDTO(
    val uuid: String,
    val createdAt: LocalDateTime,
    val status: AktivitetskravStatus,
    val vurderinger: List<AktivitetskravvurderingDTO>,
)

data class AktivitetskravvurderingDTO(
    val createdAt: LocalDateTime,
    val status: AktivitetskravStatus,
    val frist: LocalDate?,
    val varsel: AktivitetskravVarselDTO?,
    val arsaker: List<Arsak>,
)

data class AktivitetskravVarselDTO(
    val createdAt: LocalDateTime,
    val svarfrist: LocalDate?,
)

enum class AktivitetskravStatus {
    NY,
    NY_VURDERING,
    AVVENT,
    UNNTAK,
    OPPFYLT,
    AUTOMATISK_OPPFYLT,
    FORHANDSVARSEL,
    INNSTILLING_OM_STANS,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    LUKKET,
}

enum class Arsak {
    OPPFOLGINGSPLAN_ARBEIDSGIVER,
    INFORMASJON_BEHANDLER,
    INFORMASJON_SYKMELDT,
    DROFTES_MED_ROL,
    DROFTES_INTERNT,
    ANNET,
    MEDISINSKE_GRUNNER,
    TILRETTELEGGING_IKKE_MULIG,
    SJOMENN_UTENRIKS,
    FRISKMELDT,
    GRADERT,
    TILTAK,
    INNVILGET_VTA,
    MOTTAR_AAP,
    ER_DOD;
}
