package no.nav.syfo.testutil

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.testutil.mock.generatePdlPersonNavn

object UserConstants {

    const val ARBEIDSTAKER_FNR = "12345678912"
    const val ARBEIDSTAKER_2_FNR = "12345678911"
    const val ARBEIDSTAKER_3_FNR = "12345678913"
    const val ARBEIDSTAKER_4_FNR_WITH_ERROR = "12345678666"

    val ARBEIDSTAKER_NO_NAME_FNR = ARBEIDSTAKER_FNR.replace("2", "1")
    val ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT = PersonIdent(ARBEIDSTAKER_FNR.replace("2", "3"))
    val ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT = PersonIdent(ARBEIDSTAKER_FNR.replace("2", "4"))

    const val NAV_ENHET = "0330"
    const val NAV_ENHET_2 = "0331"
    const val VEILEDER_ID = "Z999999"
    const val VEILEDER_IDENT_NO_AZURE_AD_TOKEN = "Z00000_no_azure_ad_token"
    const val VIRKSOMHETSNUMMER = "123456789"
    const val VIRKSOMHETSNUMMER_2 = "123456781"

    const val VIRKSOMHETSNUMMER_3 = "123456783"

    val VIRKSOMHETSNUMMER_DEFAULT = Virksomhetsnummer(VIRKSOMHETSNUMMER)
    val VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN = Virksomhetsnummer(VIRKSOMHETSNUMMER_DEFAULT.value.replace("1", "3"))
}

fun getIdentName(): String {
    val pdlPersonNavn = generatePdlPersonNavn(ident = UserConstants.ARBEIDSTAKER_FNR)
    return "${pdlPersonNavn.fornavn} ${pdlPersonNavn.mellomnavn} ${pdlPersonNavn.etternavn}"
}
