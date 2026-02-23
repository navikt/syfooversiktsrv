package no.nav.syfo.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.api.endpoints.personOversiktApiV2Path
import no.nav.syfo.api.model.PersonOversiktStatusDTO
import no.nav.syfo.api.model.SearchQueryDTO
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelleVirksomhet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class PersonoversiktSearchApiTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)
    private val url = "$personOversiktApiV2Path/search"

    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = VEILEDER_ID,
    )
    private val fodselsdato = LocalDate.of(1985, Month.MAY, 17)

    private fun createActiveOppfolgingstilfelle(
        virksomhetsnummer: Virksomhetsnummer = Virksomhetsnummer("123456789"),
        virksomhetsnavn: String = "Virksomhet AS",
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

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Test
    fun `Returns sykmeldt person matching search when veileder has access to person`() {
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
            assertEquals(HttpStatusCode.OK, response.status)
            val personer = response.body<List<PersonOversiktStatusDTO>>()
            assertEquals(1, personer.size)
            val person = personer.first()
            assertEquals(UserConstants.ARBEIDSTAKER_FNR, person.fnr)
            assertNotNull(person.latestOppfolgingstilfelle)
            val oppfolgingstilfelleVirksomhet = person.latestOppfolgingstilfelle?.virksomhetList?.first()
            assertNotNull(oppfolgingstilfelleVirksomhet)
            assertEquals("123456789", oppfolgingstilfelleVirksomhet?.virksomhetsnummer)
            assertEquals("Virksomhet AS", oppfolgingstilfelleVirksomhet?.virksomhetsnavn)
        }
    }

    @Test
    fun `Returns sykmeldt person matching search using fodselsdato when veileder has access to person`() {
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
            assertEquals(HttpStatusCode.OK, response.status)
            val personer = response.body<List<PersonOversiktStatusDTO>>()
            assertEquals(1, personer.size)
            assertEquals(UserConstants.ARBEIDSTAKER_FNR, personer.first().fnr)
        }
    }

    @Test
    fun `Returns sykmeldte personer matching search using fodselsdato when veileder has access to personer`() {
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
            assertEquals(HttpStatusCode.OK, response.status)
            val personer = response.body<List<PersonOversiktStatusDTO>>()
            assertEquals(2, personer.size)

            val person = personer[0]
            assertEquals(UserConstants.ARBEIDSTAKER_2_FNR, person.fnr)
            assertEquals("987654321", person.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnummer)
            assertEquals("Annen Virksomhet AS", person.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnavn)

            val annenPerson = personer[1]
            assertEquals(UserConstants.ARBEIDSTAKER_FNR, annenPerson.fnr)
            assertEquals("123456789", annenPerson.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnummer)
            assertEquals("Virksomhet AS", annenPerson.latestOppfolgingstilfelle?.virksomhetList?.first()?.virksomhetsnavn)
        }
    }

    @Test
    fun `Does not return sykmeldt person not matching search when veileder has access to person`() {
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
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Returns nothing when no person matching search`() {
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
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Returns nothing when sykmeldt person matching search but veileder has no access to person`() {
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
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Returns BadRequest when not legal search query`() {
        testApplication {
            val client = setupApiAndClient()
            val searchQueryDTO = SearchQueryDTO(initials = "FE")
            val response = client.post(url) {
                bearerAuth(validToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(searchQueryDTO)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `Returns BadRequest when name is blank`() {
        testApplication {
            val client = setupApiAndClient()
            val searchQueryDTO = SearchQueryDTO(name = "")
            val response = client.post(url) {
                bearerAuth(validToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(searchQueryDTO)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `Returns BadRequest when initials is blank`() {
        testApplication {
            val client = setupApiAndClient()
            val searchQueryDTO = SearchQueryDTO(initials = "")
            val response = client.post(url) {
                bearerAuth(validToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(searchQueryDTO)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `Returns BadRequest when all parameters is null`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.post(url) {
                bearerAuth(validToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(SearchQueryDTO())
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }
}
