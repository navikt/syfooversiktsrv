package no.nav.syfo.testutil

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.personstatus.createPersonOversiktStatus
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import org.flywaydb.core.Flyway
import java.sql.Connection

class TestDatabase : DatabaseInterface {
    private val pg: EmbeddedPostgres

    override val connection: Connection
        get() = pg.postgresDatabase.connection.apply { autoCommit = false }

    init {
        pg = try {
            EmbeddedPostgres.start()
        } catch (e: Exception) {
            EmbeddedPostgres.builder().setLocaleConfig("locale", "en_US").start()
        }

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }
}

class TestDatabaseNotResponding : DatabaseInterface {

    override val connection: Connection
        get() = throw Exception("Not working")

    fun stop() {
    }
}

fun Connection.dropData() {
    val queryList = listOf(
        """
        DELETE FROM PERSON_OVERSIKT_STATUS
        """.trimIndent(),
        """
        DELETE FROM PERSON_OPPFOLGINGSTILFELLE
        """.trimIndent(),
        """
        DELETE FROM PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
        """.trimIndent(),
    )
    use { connection ->
        queryList.forEach { query ->
            connection.prepareStatement(query).execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.createPersonOversiktStatus(personOversiktStatus: PersonOversiktStatus) {
    this.connection.createPersonOversiktStatus(
        commit = true,
        personOversiktStatus = personOversiktStatus
    )
}
