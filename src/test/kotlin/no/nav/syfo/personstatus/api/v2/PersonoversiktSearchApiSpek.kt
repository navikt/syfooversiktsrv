package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.api.v2.model.SearchQueryDTO
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PersonoversiktSearchApiSpek : Spek({

    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("PersonoversiktSearchApi") {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment
            )

            val url = "$personOversiktApiV2Path/search"

            beforeEachTest {
                database.dropData()
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )

            it("returns person matching search when veileder has access to person") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn")
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FE")

                with(
                    handleRequest(HttpMethod.Post, url) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        setBody(objectMapper.writeValueAsString(searchQueryDTO))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                    val personer = objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!)
                    personer.size shouldBeEqualTo 1
                    personer.first().fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
                }
            }

            it("returns nothing when no person matching search") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn")
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "AB")

                with(
                    handleRequest(HttpMethod.Post, url) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        setBody(objectMapper.writeValueAsString(searchQueryDTO))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.NoContent
                }
            }

            it("returns nothing when person matching search but veileder has no access to person") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS, navn = "Fornavn Etternavn")
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FE")

                with(
                    handleRequest(HttpMethod.Post, url) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        setBody(objectMapper.writeValueAsString(searchQueryDTO))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.NoContent
                }
            }
        }
    }
})
