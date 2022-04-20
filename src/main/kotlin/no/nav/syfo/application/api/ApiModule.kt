package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.registerPersonTildelingApiV2
import no.nav.syfo.personstatus.api.v2.registerPersonoversiktApiV2
import redis.clients.jedis.*

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownVeilederV2: WellKnown,
) {
    installCallId()
    installContentNegotiation()
    installMetrics()
    installStatusPages()

    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azure.appClientId),
                jwtIssuerType = JwtIssuerType.VEILEDER_V2,
                wellKnown = wellKnownVeilederV2,
            )
        )
    )

    val personTildelingService = PersonTildelingService(
        database = database,
    )

    val redisStore = RedisStore(
        jedisPool = JedisPool(
            JedisPoolConfig(),
            environment.redisHost,
            environment.redisPort,
            Protocol.DEFAULT_TIMEOUT,
            environment.redisSecret,
        ),
    )

    val azureAdClient = AzureAdClient(
        azureEnviroment = environment.azure,
        redisStore = redisStore,
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        pdlClientId = environment.pdlClientId,
        pdlBaseUrl = environment.pdlUrl,
        redisStore = redisStore,
    )

    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
        pdlClient = pdlClient,
    )

    val syfotilgangskontrollClientId = environment.syfotilgangskontrollClientId
    val tilgangskontrollConsumer = VeilederTilgangskontrollClient(
        endpointUrl = environment.syfotilgangskontrollUrl,
        azureAdClient = azureAdClient,
        syfotilgangskontrollClientId = syfotilgangskontrollClientId,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()
        authenticate(JwtIssuerType.VEILEDER_V2.name) {
            registerPersonoversiktApiV2(tilgangskontrollConsumer, personoversiktStatusService)
            registerPersonTildelingApiV2(tilgangskontrollConsumer, personTildelingService)
        }
    }
}
