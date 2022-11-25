package no.nav.syfo.pdlpersonhendelse.kafka

import java.time.LocalDate

data class KafkaPersonhendelseDTO(
    val hendelseId: String,
    val personidenter: List<String>,
    val master: String,
    val opprettet: String, // TODO: egentlig timestamp
    val opplysningstype: String,
    val endringstype: Endringstype,
    val tidligereHendelseId: String? = null,
    val adressebeskyttelse: String? = null,
    val doedfoedtBarn: String? = null,
    val doedsfall: String? = null,
    val foedsel: String? = null,
    val forelderBarnRelasjon: String? = null,
    val familierelasjon: String? = null,
    val sivilstand: String? = null,
    val vergemaalEllerFremtidsfullmakt: String? = null,
    val utflyttingFraNorge: String? = null,
    val InnflyttingTilNorge: String? = null,
    val Folkeregisteridentifikator: String? = null,
    val navn: Navn? = null,
    val sikkerhetstiltak: String? = null,
    val statsborgerskap: String? = null,
    val telefonnummer: String? = null,
    val kontaktadresse: String? = null,
    val bostedsadresse: String? = null,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val forkortetNavn: String? = null,
    val originaltNavn: OriginaltNavn? = null,
    val gyldigFraOgMed: LocalDate? = null,
)

data class OriginaltNavn(
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
)

enum class Endringstype {
    OPPRETTET,
    KORRIGERT,
    ANNULLERT,
    OPPHOERT,
}
