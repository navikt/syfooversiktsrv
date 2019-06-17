package no.nav.syfo.testutil

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
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
    use { connection ->
        connection.prepareStatement("DELETE FROM PERSON_OVERSIKT_STATUS").executeUpdate()
        connection.commit()
    }
}

fun Connection.opprettVeilederBrukerKnytning(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val query = """INSERT INTO PERSON_OVERSIKT_STATUS (
            id,
            uuid,
            fnr,
            tildelt_veileder,
            tildelt_enhet,
            opprettet,
            sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)"""

    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    use { connection ->
        connection.prepareStatement(query).use {
            it.setString(1, uuid)
            it.setString(2, veilederBrukerKnytning.fnr)
            it.setString(3, veilederBrukerKnytning.veilederIdent)
            it.setString(4, veilederBrukerKnytning.enhet)
            it.setTimestamp(5, tidspunkt)
            it.setTimestamp(6, tidspunkt)
            it.execute().toString()
        }
        connection.commit()
    }
}
