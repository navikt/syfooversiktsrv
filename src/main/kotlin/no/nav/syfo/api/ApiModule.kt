package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.api.authentication.*
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.personstatus.*
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import java.net.URL
import java.util.concurrent.TimeUnit

fun Application.apiModule() {
    val env = getEnvironment()

    installCallId()
    installContentNegotiation()
    installStatusPages()

    install(Authentication) {
        val wellKnown = getWellKnown(env.aadDiscoveryUrl)
        val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
        jwt(name = "jwt") {
            verifier(jwkProvider, wellKnown.issuer)
            validate { credentials ->
                if (!credentials.payload.audience.contains(env.clientid)) {
                    log.warn(
                        "Auth: Unexpected audience for jwt {}, {}, {}",
                        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
                        StructuredArguments.keyValue("audience", credentials.payload.audience),
                        StructuredArguments.keyValue("expectedAudience", env.clientid)
                    )
                    null
                } else {
                    JWTPrincipal(credentials.payload)
                }
            }
        }
    }

    isProd {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.contains(Regex("is_alive|is_ready|prometheus"))) {
                proceed()
                return@intercept
            }
            val cookies = call.request.cookies
            if (isInvalidToken(cookies)) {
                call.respond(HttpStatusCode.Unauthorized, "Ugyldig token")
                finish()
            } else {
                proceed()
            }
        }
    }

    val personTildelingService = PersonTildelingService(database)
    val personoversiktStatusService = PersonoversiktStatusService(database)
    val tilgangskontrollConsumer = TilgangskontrollConsumer(env.syfotilgangskontrollUrl)

    routing {
        registerPodApi(state)
        registerPrometheusApi()
        registerPersonoversiktApi(tilgangskontrollConsumer, personoversiktStatusService)
        registerPersonTildelingApi(tilgangskontrollConsumer, personTildelingService)
    }

    state.initialized = true
}
