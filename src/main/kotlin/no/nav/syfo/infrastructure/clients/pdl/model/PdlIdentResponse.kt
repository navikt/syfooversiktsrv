package no.nav.syfo.infrastructure.clients.pdl.model

import kotlin.collections.firstOrNull
import kotlin.collections.none

data class PdlIdentResponse(
    val errors: List<PdlError>?,
    val data: PdlHentIdenter?,
)

data class PdlHentIdenter(
    val hentIdenter: PdlIdenter?,
)

data class PdlIdenter(
    val identer: List<PdlIdent>,
) {
    val aktivIdent: String? = identer.firstOrNull {
        it.gruppe == IdentGruppe.FOLKEREGISTERIDENT && !it.historisk
    }?.ident

    fun identhendelseIsNotHistorisk(newIdent: String): Boolean {
        return identer.none { it.ident == newIdent && it.historisk }
    }
}

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: IdentGruppe,
)

enum class IdentGruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}
