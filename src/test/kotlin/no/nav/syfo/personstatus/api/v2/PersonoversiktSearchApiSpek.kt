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
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.Month

object PersonoversiktSearchApiSpek : Spek({

    val objectMapper: ObjectMapper = configuredJacksonMapper()

    val activeOppfolgingstilfelle = generateOppfolgingstilfelle(
        start = LocalDate.now().minusWeeks(15),
        end = LocalDate.now().plusWeeks(1),
        antallSykedager = null,
    )

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
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)

            it("returns sykmeldt person matching search when veileder has access to person") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FE", birthdate = fodselsdato)

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

            it("returns sykmeldt person matching search using fodselsdato when veileder has access to person") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = null, birthdate = fodselsdato)

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
            it("returns sykmeldte personer matching search using fodselsdato when veileder has access to personer") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                val newPersonOversiktStatus2 =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_2_FNR, navn = "Firstname Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                val newPersonOversiktStatus3 =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS, navn = "Forstname Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus2)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus3)
                val searchQueryDTO = SearchQueryDTO(initials = null, birthdate = fodselsdato)

                with(
                    handleRequest(HttpMethod.Post, url) {
                        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        setBody(objectMapper.writeValueAsString(searchQueryDTO))
                    }
                ) {
                    response.status() shouldBeEqualTo HttpStatusCode.OK
                    val personer = objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!)
                    personer.size shouldBeEqualTo 2
                    personer[0].fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_2_FNR
                    personer[1].fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
                }
            }

            it("does not return sykmeldt person not matching search when veileder has access to person") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FN", birthdate = fodselsdato)

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

            it("returns nothing when no person matching search") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, navn = "Fornavn Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "AB", birthdate = LocalDate.now())

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

            it("returns nothing when sykmeldt person matching search but veileder has no access to person") {
                val newPersonOversiktStatus =
                    PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS, navn = "Fornavn Etternavn", fodselsdato = fodselsdato, latestOppfolgingstilfelle = activeOppfolgingstilfelle)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FE", birthdate = fodselsdato)

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
