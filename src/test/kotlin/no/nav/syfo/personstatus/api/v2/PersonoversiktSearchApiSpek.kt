package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
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
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.Month

object PersonoversiktSearchApiSpek : Spek({

    val activeOppfolgingstilfelle = generateOppfolgingstilfelle(
        start = LocalDate.now().minusWeeks(15),
        end = LocalDate.now().plusWeeks(1),
        antallSykedager = null,
    )

    describe("PersonoversiktSearchApi") {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)

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
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FE", birthdate = fodselsdato)

                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK
                val personer = response.body<List<PersonOversiktStatusDTO>>()
                personer.size shouldBeEqualTo 1
                personer.first().fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
            }
        }

        it("returns sykmeldt person matching search using fodselsdato when veileder has access to person") {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "", birthdate = fodselsdato)
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK
                val personer = response.body<List<PersonOversiktStatusDTO>>()
                personer.size shouldBeEqualTo 1
                personer.first().fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
            }
        }
        it("returns sykmeldte personer matching search using fodselsdato when veileder has access to personer") {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                val newPersonOversiktStatus2 =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                        navn = "Firstname Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                val newPersonOversiktStatus3 =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS,
                        navn = "Forstname Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus2)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus3)
                val searchQueryDTO = SearchQueryDTO(birthdate = fodselsdato)
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK
                val personer = response.body<List<PersonOversiktStatusDTO>>()
                personer.size shouldBeEqualTo 2
                personer[0].fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_2_FNR
                personer[1].fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
            }
        }

        it("does not return sykmeldt person not matching search when veileder has access to person") {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FN", birthdate = fodselsdato)
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.NoContent
            }
        }

        it("returns nothing when no person matching search") {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "AB", birthdate = LocalDate.now())
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.NoContent
            }
        }

        it("returns nothing when sykmeldt person matching search but veileder has no access to person") {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                val searchQueryDTO = SearchQueryDTO(initials = "FE", birthdate = fodselsdato)
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.NoContent
            }
        }
        it("returns BadRequest when not legal search query") {
            testApplication {
                val client = setupApiAndClient()
                val searchQueryDTO = SearchQueryDTO(initials = "FE")
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }

        it("returns BadRequest when not legal search query") {
            testApplication {
                val client = setupApiAndClient()
                val searchQueryDTO = SearchQueryDTO(initials = "FE")
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
        it("returns BadRequest when name is blank") {
            testApplication {
                val client = setupApiAndClient()
                val searchQueryDTO = SearchQueryDTO(name = "")
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
        it("returns BadRequest when initials is blank") {
            testApplication {
                val client = setupApiAndClient()
                val searchQueryDTO = SearchQueryDTO(name = "")
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
        it("returns BadRequest when all parameters is null") {
            testApplication {
                val client = setupApiAndClient()
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(SearchQueryDTO())
                }
                response.status shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }
    }
})
