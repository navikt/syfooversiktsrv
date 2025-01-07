package no.nav.syfo.testutil

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime

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

fun DatabaseInterface.dropData() {
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
    this.connection.use { connection ->
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

const val queryUpdateTildeltEnhetAt =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_enhet_updated_at = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updateTildeltEnhetUpdatedAt(
    ident: PersonIdent,
    time: OffsetDateTime,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateTildeltEnhetAt).use {
            it.setObject(1, time)
            it.setString(2, ident.value)
            it.execute()
        }
        connection.commit()
    }
}

const val querySetTildeltEnhet =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_enhet = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.setTildeltEnhet(
    ident: PersonIdent,
    enhet: String?,
) {
    this.connection.use { connection ->
        connection.prepareStatement(querySetTildeltEnhet).use {
            it.setString(1, enhet)
            it.setString(2, ident.value)
            if (it.executeUpdate() != 1) throw RuntimeException("No row updated")
        }
        connection.commit()
    }
}

const val queryFriskmeldingtilarbeidsformidlingFom =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET friskmelding_til_arbeidsformidling_fom = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.setFriskmeldingTilArbeidsformidlingFom(
    ident: PersonIdent,
    fom: LocalDate,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryFriskmeldingtilarbeidsformidlingFom).use {
            it.setObject(1, fom)
            it.setString(2, ident.value)
            if (it.executeUpdate() != 1) throw RuntimeException("No row updated")
        }
        connection.commit()
    }
}

const val querySetSistEndret =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.setSistEndret(fnr: String, sistEndret: Timestamp) {
    this.connection.use { connection ->
        connection.prepareStatement(querySetSistEndret).use {
            it.setTimestamp(1, sistEndret)
            it.setString(2, fnr)
            it.executeUpdate()
        }
        connection.commit()
    }
}
