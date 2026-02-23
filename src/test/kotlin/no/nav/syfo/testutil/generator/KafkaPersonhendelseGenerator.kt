package no.nav.syfo.testutil.generator

import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.navn.Navn
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.testutil.UserConstants
import java.time.Instant
import java.time.LocalDate

val defaultNavn = Navn(
    "Testfornavn",
    null,
    "Testetternavn",
    null,
    null,
    LocalDate.now().minusDays(1)
)

fun generateKafkaPersonhendelse(
    personIdent: PersonIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
    navn: Navn? = defaultNavn,
) = Personhendelse(
    "123",
    listOf(personIdent.value),
    "FREG",
    Instant.now(),
    "Navn",
    Endringstype.OPPRETTET,
    null,
    navn,
)
