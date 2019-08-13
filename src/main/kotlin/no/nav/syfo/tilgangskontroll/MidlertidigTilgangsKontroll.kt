package no.nav.syfo.tilgangskontroll

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.LOG
import no.nav.syfo.util.allToUpperCase
import java.nio.file.Paths

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
}

data class Tilganger (
        val identer: List<String>
)

private fun lesTilgangsfil(path: String): Tilganger {
    LOG.info("Leser tilgangsfil fra $path")
    val s = Paths.get(path).toFile().readText()
    return objectMapper.readValue<Tilganger>(s).also { LOG.info("Leste tilgang fra fil med ${it.identer.size} identer") }
}

private const val vaultFile = "/var/run/secrets/nais.io/vault/tilganger.json"

class MidlertidigTilgangsSjekk(pathTilTilgangsfil: String = vaultFile) {

    var tilgangListe = arrayListOf<String>()

    init {
        val tilgangsFil = lesTilgangsfil(pathTilTilgangsfil)
        tilgangListe.addAll(tilgangsFil.identer.allToUpperCase())
    }

    fun harTilgang(navIdent: String): Boolean = tilgangListe.contains(navIdent.toUpperCase())
}
