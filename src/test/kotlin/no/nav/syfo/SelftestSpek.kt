package no.nav.syfo

import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.registerPodApi
import no.nav.syfo.application.api.registerPrometheusApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SelftestSpek : Spek({
    val applicationState = ApplicationState()

    describe("Calling selftest with successful liveness and readyness tests") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerPodApi(applicationState)
                registerPrometheusApi()
            }

            it("Returns ok on is_alive") {
                applicationState.alive = true

                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status()?.isSuccess() shouldBeEqualTo true
                    response.content shouldNotBeEqualTo null
                }
            }
            it("Returns ok on is_ready") {
                applicationState.ready = true

                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    println(response.status())
                    response.status()?.isSuccess() shouldBeEqualTo true
                    response.content shouldNotBeEqualTo null
                }
            }
            it("Returns error on failed is_alive") {
                applicationState.alive = false

                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status()?.isSuccess() shouldNotBeEqualTo true
                    response.content shouldNotBeEqualTo null
                }
            }
            it("Returns error on failed is_ready") {
                applicationState.ready = false

                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    response.status()?.isSuccess() shouldNotBeEqualTo true
                    response.content shouldNotBeEqualTo null
                }
            }
        }
    }

    describe("Calling selftests with unsucessful liveness test") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerPodApi(ApplicationState(alive = false))
            }

            it("Returns internal server error when liveness check fails") {
                with(handleRequest(HttpMethod.Get, "/is_alive")) {
                    response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                    response.content shouldNotBeEqualTo null
                }
            }
        }
    }

    describe("Calling selftests with unsucessful readyness test") {
        with(TestApplicationEngine()) {
            start()
            application.routing {
                registerPodApi(ApplicationState(ready = false))
            }

            it("Returns internal server error when readyness check fails") {
                with(handleRequest(HttpMethod.Get, "/is_ready")) {
                    response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                    response.content shouldNotBeEqualTo null
                }
            }
        }
    }
})
