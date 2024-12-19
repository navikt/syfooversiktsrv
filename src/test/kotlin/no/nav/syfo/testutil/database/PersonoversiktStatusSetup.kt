package no.nav.syfo.testutil.database

import io.ktor.server.application.*
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import java.time.OffsetDateTime

fun setAsKandidat(
    database: TestDatabase,
    kandidatGeneratedAt: OffsetDateTime = OffsetDateTime.now().minusDays(10),
) {
    val ppersonOversiktStatus = generatePPersonOversiktStatus()

    database.connection.use { connection ->
        connection.updatePersonOversiktStatusKandidat(
            pPersonOversiktStatus = ppersonOversiktStatus,
            kandidat = true,
            generatedAt = kandidatGeneratedAt
        )
        connection.commit()
    }
}

fun setTildeltEnhet(database: TestDatabase) {
    database.setTildeltEnhet(
        ident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
        enhet = UserConstants.NAV_ENHET,
    )
}

fun setDialogmotestatus(
    database: TestDatabase,
    status: DialogmoteStatusendringType = DialogmoteStatusendringType.INNKALT,
) {
    val ppersonOversiktStatus = generatePPersonOversiktStatus()
    val statusendring = DialogmoteStatusendring.create(
        generateKafkaDialogmoteStatusendring(
            personIdent = UserConstants.ARBEIDSTAKER_FNR,
            type = status,
            endringsTidspunkt = OffsetDateTime.now().minusDays(1)
        )
    )
    database.connection.use { connection ->
        connection.updatePersonOversiktStatusMotestatus(
            pPersonOversiktStatus = ppersonOversiktStatus,
            dialogmoteStatusendring = statusendring,
        )
        connection.commit()
    }
}

fun createPersonoversiktStatusWithTilfelle(database: TestDatabase) {
    val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson(
        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
        virksomhetsnummerList = listOf(
            UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
            Virksomhetsnummer(UserConstants.VIRKSOMHETSNUMMER_2),
        )
    )
    val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
    database.createPersonOversiktStatus(
        personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(kafkaOppfolgingstilfelle)
    )
}
