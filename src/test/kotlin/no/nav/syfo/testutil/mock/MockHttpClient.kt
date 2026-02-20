package no.nav.syfo.testutil.mock

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import no.nav.syfo.Environment
import no.nav.syfo.infrastructure.clients.commonConfig

fun mockHttpClient(environment: Environment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.encodedPath
            when {
                requestUrl == "/${environment.azure.openidConfigTokenEndpoint}" -> azureAdMockResponse(request)
                requestUrl.startsWith("/${environment.clients.istilgangskontroll.baseUrl}") -> tilgangskontrollResponse(
                    request
                )
                requestUrl.startsWith("/${environment.clients.syfobehandlendeenhet.baseUrl}") -> {
                    getBehandlendeEnhetResponse(request)
                }
                requestUrl.startsWith("/${environment.clients.pdl.baseUrl}") -> pdlMockResponse(request)
                requestUrl.startsWith("/${environment.clients.ereg.baseUrl}") -> eregMockResponse(request)
                requestUrl.startsWith("/${environment.clients.arbeidsuforhetvurdering.baseUrl}") -> arbeidsuforhetVurderingMockResponse()
                requestUrl.startsWith("/${environment.clients.ishuskelapp.baseUrl}") -> oppfolgingsoppgaveMockResponse()
                requestUrl.startsWith("/${environment.clients.manglendeMedvirkning.baseUrl}") -> manglendeMedvirkningMockResponse()
                requestUrl.startsWith("/${environment.clients.aktivitetskrav.baseUrl}") -> aktivitetskravMockResponse()
                requestUrl.startsWith("/${environment.clients.dialogmotekandidat.baseUrl}") -> dialogmotekandidatMockResponse(request)
                requestUrl.startsWith("/${environment.clients.ismeroppfolging.baseUrl}") -> merOppfolgingMockResponse()
                requestUrl.startsWith("/${environment.clients.syfoveileder.baseUrl}") -> veilederMockResponse(request)

                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
