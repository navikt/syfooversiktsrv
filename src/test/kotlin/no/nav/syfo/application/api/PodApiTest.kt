package no.nav.syfo.application.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.api.v2.endpoints.podLivenessPath
import no.nav.syfo.personstatus.api.v2.endpoints.podReadinessPath
import no.nav.syfo.personstatus.api.v2.endpoints.registerPodApi
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.testutil.TestDatabase
import no.nav.syfo.testutil.TestDatabaseNotResponding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PodApiTest {

    private fun ApplicationTestBuilder.setupPodApi(database: DatabaseInterface, applicationState: ApplicationState) {
        application {
            routing {
                registerPodApi(
                    applicationState = applicationState,
                    database = database,
                )
            }
        }
    }

    @Nested
    @DisplayName("Successful liveness and readiness checks")
    inner class SuccessfulChecks {
        private val database = TestDatabase()

        @Test
        fun `Returns ok on is_alive`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true),
                )
                val response = client.get(podLivenessPath)
                assertEquals(HttpStatusCode.OK, response.status)
                assertNotNull(response.bodyAsText())
            }
        }

        @Test
        fun `Returns ok on is_ready`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true),
                )
                val response = client.get(podReadinessPath)
                assertEquals(HttpStatusCode.OK, response.status)
                assertNotNull(response.bodyAsText())
            }
        }
    }

    @Nested
    @DisplayName("Unsuccessful liveness and readiness checks")
    inner class UnsuccessfulChecks {
        private val database = TestDatabase()

        @Test
        fun `Returns internal server error when liveness check fails`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = false, ready = false),
                )
                val response = client.get(podLivenessPath)
                assertEquals(HttpStatusCode.InternalServerError, response.status)
                assertNotNull(response.bodyAsText())
            }
        }

        @Test
        fun `Returns internal server error when readiness check fails`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = false, ready = false),
                )
                val response = client.get(podReadinessPath)
                assertEquals(HttpStatusCode.InternalServerError, response.status)
                assertNotNull(response.bodyAsText())
            }
        }
    }

    @Nested
    @DisplayName("Successful liveness and unsuccessful readiness checks when database not working")
    inner class DatabaseNotWorking {
        private val database = TestDatabaseNotResponding()

        @Test
        fun `Returns ok on is_alive`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true),
                )
                val response = client.get(podLivenessPath)
                assertEquals(HttpStatusCode.OK, response.status)
                assertNotNull(response.bodyAsText())
            }
        }

        @Test
        fun `Returns internal server error when readiness check fails`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true),
                )
                val response = client.get(podReadinessPath)
                assertEquals(HttpStatusCode.InternalServerError, response.status)
                assertNotNull(response.bodyAsText())
            }
        }
    }
}
