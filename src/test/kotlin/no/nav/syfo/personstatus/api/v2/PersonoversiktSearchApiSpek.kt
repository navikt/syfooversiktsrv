package no.nav.syfo.personstatus.api.v2
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.api.v2.model.SearchQueryDTO
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelleVirksomhet
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class PersonoversiktSearchApiTest {
    private fun createActiveOppfolgingstilfelle(
        virksomhetsnummer: Virksomhetsnummer = Virksomhetsnummer("123456789"),
        virksomhetsnavn: String = "Virksomhet AS"
    ) = generateOppfolgingstilfelle(
        start = LocalDate.now().minusWeeks(15),
        end = LocalDate.now().plusWeeks(1),
        antallSykedager = null,
        virksomhetList = listOf(
            generateOppfolgingstilfelleVirksomhet(
                virksomhetsnummer = virksomhetsnummer,
                virksomhetsnavn = virksomhetsnavn,
            ),
        )
    )

    @Nested
    inner class PersonoversiktSearchApi {
        @Test
        fun `returns sykmeldt person matching search when veileder has access to person`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
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
                val person = personer.first()
                person.fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
                person.latestOppfolgingstilfelle.shouldNotBeNull()
                val oppfolgingstilfelleVirksomhet = person.latestOppfolgingstilfelle?.virksomhetList?.first()
                oppfolgingstilfelleVirksomhet.shouldNotBeNull()
                oppfolgingstilfelleVirksomhet.virksomhetsnummer shouldBeEqualTo "123456789"
                oppfolgingstilfelleVirksomhet.virksomhetsnavn shouldBeEqualTo "Virksomhet AS"
            }
        }

        @Test
        fun `returns sykmeldt person matching search using fodselsdato when veileder has access to person`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
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

        @Test
        fun `returns sykmeldte personer matching search using fodselsdato when veileder has access to personer`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
                    )
                val newPersonOversiktStatus2 =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                        navn = "Firstname Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(
                            virksomhetsnummer = Virksomhetsnummer("987654321"),
                            virksomhetsnavn = "Annen Virksomhet AS"
                        ),
                    )
                val newPersonOversiktStatus3 =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS,
                        navn = "Forstname Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
                    )
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus2)
                personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus3)

                val searchQueryDTO = SearchQueryDTO(initials = null, birthdate = fodselsdato)
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.OK
                val personer = response.body<List<PersonOversiktStatusDTO>>()
                personer.size shouldBeEqualTo 2

                val person = personer[0]
                person.fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_2_FNR
                person.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnummer shouldBeEqualTo "987654321"
                person.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnavn shouldBeEqualTo "Annen Virksomhet AS"

                val annenPerson = personer[1]
                annenPerson.fnr shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
                annenPerson.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnummer shouldBeEqualTo "123456789"
                annenPerson.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnavn shouldBeEqualTo "Virksomhet AS"
            }
        }

        @Test
        fun `does not return sykmeldt person not matching search when veileder has access to person`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
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

        @Test
        fun `returns nothing when no person matching search`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
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

        @Test
        fun `returns nothing when sykmeldt person matching search but veileder has no access to person`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus =
                    PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_NO_ACCESS,
                        navn = "Fornavn Etternavn",
                        fodselsdato = fodselsdato,
                        latestOppfolgingstilfelle = createActiveOppfolgingstilfelle(),
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

        @Test
        fun `returns BadRequest when not legal search query`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            database.dropData()
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

        @Test
        fun `returns BadRequest when name is blank`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            database.dropData()
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

        @Test
        fun `returns BadRequest when initials is blank`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            database.dropData()
            testApplication {
                val client = setupApiAndClient()
                val searchQueryDTO = SearchQueryDTO(initials = "")
                val response = client.post(url) {
                    bearerAuth(validToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(searchQueryDTO)
                }
                response.status shouldBeEqualTo HttpStatusCode.BadRequest
            }
        }

        @Test
        fun `returns BadRequest when all parameters is null`() {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
            val url = "$personOversiktApiV2Path/search"
            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )
            database.dropData()
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
}
