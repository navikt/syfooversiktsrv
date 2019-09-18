package no.nav.syfo.testutil

import no.nav.syfo.db.*
import no.nav.syfo.oversikthendelsetilfelle.domain.PersonOppfolgingstilfelleInternal
import no.nav.syfo.oversikthendelsetilfelle.toPersonOppfolgingstilfelleInternal
import no.nav.syfo.personstatus.*
import no.nav.syfo.personstatus.domain.*
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class TestDB : DatabaseInterface {

    val container = PostgreSQLContainer<Nothing>("postgres:11.1").apply {
        withDatabaseName("db_test")
        withUsername("username")
        withPassword("password")
    }

    private var db: DatabaseInterface
    override val connection: Connection
        get() = db.connection.apply { autoCommit = false }

    init {
        container.start()
        db = DevDatabase(DbConfig(jdbcUrl = container.jdbcUrl, username = "username", password = "password", databaseName = "db_test"))
    }

    fun stop() {
        container.stop()
    }
}

fun Connection.dropData() {
    val query = "DELETE FROM PERSON_OVERSIKT_STATUS"
    use { connection ->
        connection.prepareStatement(query).executeUpdate()
        connection.commit()
    }
}

fun Connection.opprettVeilederBrukerKnytning(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    use { connection ->
        connection.prepareStatement(queryLagreBrukerKnytningPaEnhet).use {
            it.setString(1, uuid)
            it.setString(2, veilederBrukerKnytning.fnr)
            it.setString(3, veilederBrukerKnytning.veilederIdent)
            it.setString(4, veilederBrukerKnytning.enhet)
            it.setTimestamp(5, tidspunkt)
            it.setTimestamp(6, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}


fun Connection.tildelVeilederTilPerson(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val updateQuery = """
                         UPDATE PERSON_OVERSIKT_STATUS
                         SET tildelt_veileder = ?
                         WHERE fnr = ?
                """
    use { connection ->
        connection.prepareStatement(updateQuery).use {
            it.setString(1, veilederBrukerKnytning.veilederIdent)
            it.setString(2, veilederBrukerKnytning.fnr)
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun Connection.hentPersonerTilknyttetEnhet(enhet: String): List<PersonOversiktStatus> {
    return use { connection ->
        connection.prepareStatement(queryHentPersonerTilknyttetEnhet).use {
            it.setString(1, enhet)
            it.executeQuery().toList { toPersonOversiktStatus() }
        }
    }
}

fun Connection.hentPersonResultatInternal(fnr: String): List<PersonOversiktStatusInternal> {
    return use { connection ->
        connection.prepareStatement(queryHentPersonResultatInternal).use {
            it.setString(1, fnr)
            it.executeQuery().toList { toPersonOversiktStatusInternal() }
        }
    }
}

const val queryHentOppfolgingstilfelleResultatForPerson = """
                         SELECT *
                         FROM PERSON_OPPFOLGINGSTILFELLE
                         WHERE person_oversikt_status_id = ?
                """

fun Connection.hentOppfolgingstilfelleResultat(personId: Int): List<PersonOppfolgingstilfelleInternal> {
    return use { connection ->
        connection.prepareStatement(queryHentOppfolgingstilfelleResultatForPerson).use {
            it.setInt(1, personId)
            it.executeQuery().toList { toPersonOppfolgingstilfelleInternal() }
        }
    }
}

fun Connection.opprettPerson(oversiktHendelse: KOversikthendelse) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    use { connection ->
        connection.prepareStatement(queryOpprettPersonMedMotebehovMottatt).use {
            it.setString(1, uuid)
            it.setString(2, oversiktHendelse.fnr)
            it.setString(3, oversiktHendelse.enhetId)
            it.setBoolean(4, true)
            it.setTimestamp(5, tidspunkt)
            it.setTimestamp(6, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}

fun Connection.opprettPersonMedMoteplanleggerAlleSvarMottatt(oversiktHendelse: KOversikthendelse) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    use { connection ->
        connection.prepareStatement(queryOpprettPersonMedMoteplanleggerAlleSvar).use {
            it.setString(1, uuid)
            it.setString(2, oversiktHendelse.fnr)
            it.setString(3, oversiktHendelse.enhetId)
            it.setBoolean(4, true)
            it.setTimestamp(5, tidspunkt)
            it.setTimestamp(6, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}

fun Connection.oppdaterPersonMedMotebehovMottatt(oversiktHendelse: KOversikthendelse) {
    val tidspunkt = Timestamp.from(Instant.now())

    use { connection ->
        connection.prepareStatement(queryOppdaterPersonMedMotebehovMottatt).use {
            it.setBoolean(1, true)
            it.setString(2, oversiktHendelse.enhetId)
            it.setTimestamp(3, tidspunkt)
            it.setString(4, oversiktHendelse.fnr)
            it.execute()
        }
        connection.commit()
    }
}
