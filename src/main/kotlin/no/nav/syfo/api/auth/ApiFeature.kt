package no.nav.syfo.api.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import io.ktor.util.cio.ChannelWriteException
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.access.ForbiddenAccessSystemConsumer
import no.nav.syfo.infrastructure.METRICS_REGISTRY
import no.nav.syfo.util.*
import no.nav.syfo.util.configure
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getConsumerId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.any
import kotlin.collections.forEach
import kotlin.text.isNotEmpty

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.api.authentication")

fun Application.installJwtAuthentication(
    jwtIssuerList: List<JwtIssuer>,
) {
    install(Authentication) {
        jwtIssuerList.forEach { jwtIssuer ->
            configureJwt(
                jwtIssuer = jwtIssuer
            )
        }
    }
}

fun AuthenticationConfig.configureJwt(
    jwtIssuer: JwtIssuer,
) {
    val jwkProvider = JwkProviderBuilder(URL(jwtIssuer.wellKnown.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    jwt(name = jwtIssuer.jwtIssuerType.name) {
        verifier(jwkProvider, jwtIssuer.wellKnown.issuer)
        validate { credential ->
            if (hasExpectedAudience(credential, jwtIssuer.acceptedAudienceList)) {
                JWTPrincipal(credential.payload)
            } else {
                log.warn(
                    "Auth: Unexpected audience for jwt {}, {}",
                    StructuredArguments.keyValue("issuer", credential.payload.issuer),
                    StructuredArguments.keyValue("audience", credential.payload.audience)
                )
                null
            }
        }
    }
}

fun hasExpectedAudience(
    credentials: JWTCredential,
    expectedAudience: List<String>,
): Boolean {
    return expectedAudience.any { credentials.payload.audience.contains(it) }
}

fun Application.installCallId() {
    install(CallId) {
        retrieve { it.request.headers[NAV_CALL_ID_HEADER] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }
}

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson { configure() }
    }
}

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val callId = call.getCallId()
            val consumerId = call.getConsumerId()
            val exceptionMessage = "Caught exception, callId=$callId, consumerClientId=$consumerId"
            if (cause is ChannelWriteException) {
                call.application.log.warn(exceptionMessage, cause)
            } else {
                call.application.log.error(exceptionMessage, cause)
            }

            var isUnexpectedException = false

            val responseStatus: HttpStatusCode = when (cause) {
                is ResponseException -> {
                    cause.response.status
                }
                is IllegalArgumentException -> {
                    HttpStatusCode.BadRequest
                }
                is ForbiddenAccessSystemConsumer -> {
                    HttpStatusCode.Forbidden
                }
                else -> {
                    isUnexpectedException = true
                    HttpStatusCode.InternalServerError
                }
            }
            val message = if (isUnexpectedException) {
                "The server reported an unexpected error and cannot complete the request."
            } else {
                cause.message ?: "Unknown error"
            }
            call.respond(responseStatus, message)
        }
    }
}

fun Application.installMetrics() {
    install(MicrometerMetrics) {
        registry = METRICS_REGISTRY
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .maximumExpectedValue(Duration.ofSeconds(20).toNanos().toDouble())
            .build()
    }
}
