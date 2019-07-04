package no.nav.syfo.testutil

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.personstatus.*
import no.nav.syfo.personstatus.domain.*
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class TestDB : DatabaseInterface {
    private var pg: EmbeddedPostgres? = null
    override val connection: Connection
        get() = pg!!.postgresDatabase.connection.apply { autoCommit = false }

    init {
        pg = EmbeddedPostgres.start()
        Flyway.configure().run {
            dataSource(pg?.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg?.close()
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

fun Connection.hentPersonerTilknyttetEnhet(enhet: String): List<PersonOversiktStatus> {
    return use { connection ->
        connection.prepareStatement(queryHentPersonerTilknyttetEnhet).use {
            it.setString(1, enhet)
            it.executeQuery().toList { toPersonOversiktStatus() }
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
