package no.nav.syfo.personstatus

import io.ktor.client.request.*
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import no.nav.syfo.personstatus.domain.Veileder

class VeilederConsumer(
        private val endpointUrl: String,
        private val client: HttpClient
) {
    val cache  = HashMap<String, ArrayList<Veileder>>()

    suspend fun hentNavn(enhet: String, ident: String, token: String): Veileder? {
        val veiledere = cache[enhet] ?: hentVeilederNavn(enhet, token)
        return veiledere.find { it.ident == ident }
    }

    //TODO: finn/lag avansert cache som kan cache i én dag
    suspend fun hentVeilederNavn(enhet: String, token: String): List<Veileder> {
        val veiledere = client.get<ArrayList<Veileder>>(getSyfoVeilederUrl("/veiledere/enhet/$enhet")) {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $token")
            }
        }
        return veiledere
    }

    private fun getSyfoVeilederUrl(path: String): String {
        return "$endpointUrl/syfoveileder/api$path"
    }
}
