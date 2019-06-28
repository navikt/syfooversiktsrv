package no.nav.syfo.testutil

import kotlinx.coroutines.awaitAll
import no.nav.syfo.db.DaoConfig
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.DevDatabase
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import org.testcontainers.containers.GenericContainer
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
        db = DevDatabase(DaoConfig(jdbcUrl = container.jdbcUrl, username = "username", password = "password", databaseName = "db_test"))
//        pg = EmbeddedPostgres.builder().setDataDirectory("/var/tmp/syfooversikt_test_data").start()

    }

    fun stop() {
        container.stop()
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
