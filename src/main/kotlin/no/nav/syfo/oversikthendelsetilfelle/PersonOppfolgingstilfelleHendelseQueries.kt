package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.PersonOppfolgingstilfelleInternal
import no.nav.syfo.util.convert
import java.sql.*
import java.sql.Types.NULL
import java.time.Instant
import java.util.*

const val queryHentOppfolgingstilfelleResultat = """
                         SELECT *
                         FROM PERSON_OPPFOLGINGSTILFELLE
                         WHERE (person_oversikt_status_id = ? AND virksomhetsnummer = ?)
                """

fun DatabaseInterface.hentOppfolgingstilfelleResultat(personId: Int, virksomhetsnummer: String): List<PersonOppfolgingstilfelleInternal> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentOppfolgingstilfelleResultat).use {
            it.setInt(1, personId)
            it.setString(2, virksomhetsnummer)
            it.executeQuery().toList { toPersonOppfolgingstilfelleInternal() }
        }
    }
}

const val queryOppdaterOppfolgingstilfelleMottatt = """
                        UPDATE PERSON_OPPFOLGINGSTILFELLE
                        SET gradert = ?, fom = ?, tom = ?, sist_endret = ?
                        WHERE person_oversikt_status_id = ?
                """

fun DatabaseInterface.oppdaterOppfolgingstilfelleMottatt(personId: Int, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOppdaterOppfolgingstilfelleMottatt).use {
            it.setBoolean(1, oversikthendelsetilfelle.gradert)
            it.setTimestamp(2, convert(oversikthendelsetilfelle.fom))
            it.setTimestamp(3, convert(oversikthendelsetilfelle.tom))
            it.setTimestamp(4, tidspunkt)
            it.setInt(5, personId)
            it.execute()
        }
        connection.commit()
    }
}

const val queryOpprettOppfolgingstilfelleMottatt = """INSERT INTO PERSON_OPPFOLGINGSTILFELLE (
        id,
        uuid,
        person_oversikt_status_id,
        virksomhetsnummer,
        fom,
        tom,
        gradert,
        opprettet,
        sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)"""

fun DatabaseInterface.opprettOppfolgingstilfelleMottatt(personId: Int, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOpprettOppfolgingstilfelleMottatt).use {
            it.setString(1, uuid)
            it.setInt(2, personId)
            it.setString(3, oversikthendelsetilfelle.virksomhetsnummer)
            it.setTimestamp(4, convert(oversikthendelsetilfelle.fom))
            it.setTimestamp(5, convert(oversikthendelsetilfelle.tom))
            it.setBoolean(6, oversikthendelsetilfelle.gradert)
            it.setTimestamp(7, tidspunkt)
            it.setTimestamp(8, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.opprettEllerOppdaterOppfolgingstilfelle(personId: Int, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val resultatListe = hentOppfolgingstilfelleResultat(personId, oversikthendelsetilfelle.virksomhetsnummer)

    if (resultatListe.isEmpty()) {
        opprettOppfolgingstilfelleMottatt(personId, oversikthendelsetilfelle)
    } else {
        oppdaterOppfolgingstilfelleMottatt(personId, oversikthendelsetilfelle)
    }
}

const val queryOpprettPersonOppfolgingstilfelleMottatt = """INSERT INTO PERSON_OVERSIKT_STATUS (
        id,
        uuid,
        fnr,
        tildelt_enhet,
        opprettet,
        sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?) RETURNING id"""

fun DatabaseInterface.opprettPersonOppfolgingstilfelleMottatt(oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    var personId: Int? = null

    connection.use { connection ->
        val personIdList = connection.prepareStatement(queryOpprettPersonOppfolgingstilfelleMottatt).use {
            it.setString(1, uuid)
            it.setString(2, oversikthendelsetilfelle.fnr)
            it.setString(3, oversikthendelsetilfelle.enhetId)
            it.setTimestamp(4, tidspunkt)
            it.setTimestamp(5, tidspunkt)
            it.executeQuery().toList { getInt("id") }
        }

        if (personIdList.size != 1) {
            throw SQLException("Creating person failed, no rows affected.")
        }

        personId = personIdList.first()

        connection.commit()
    }
    personId?.let { opprettEllerOppdaterOppfolgingstilfelle(it, oversikthendelsetilfelle) }
}

const val queryOpprettPersonOppfolgingstilfelleNyEnhetMottatt = """
                        UPDATE PERSON_OVERSIKT_STATUS
                        SET tildelt_veileder = ?, tildelt_enhet = ?, sist_endret = ?
                        WHERE fnr = ?
                """

fun DatabaseInterface.oppdaterPersonOppfolgingstilfelleNyEnhetMottatt(personId: Int, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val tidspunkt = Timestamp.from(Instant.now())
    connection.use { connection ->
        connection.prepareStatement(queryOpprettPersonOppfolgingstilfelleNyEnhetMottatt).use {
            it.setNull(1, NULL)
            it.setString(2, oversikthendelsetilfelle.enhetId)
            it.setTimestamp(3, tidspunkt)
            it.setString(4, oversikthendelsetilfelle.fnr)
            it.execute()
        }
        connection.commit()
    }
    opprettEllerOppdaterOppfolgingstilfelle(personId, oversikthendelsetilfelle)
}

const val queryOppdaterPersonOppfolgingstilfelleMottatt = """
                        UPDATE PERSON_OVERSIKT_STATUS
                        SET tildelt_enhet = ?, sist_endret = ?
                        WHERE fnr = ?
                """

fun DatabaseInterface.oppdaterPersonOppfolgingstilfelleMottatt(personId: Int, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val tidspunkt = Timestamp.from(Instant.now())
    connection.use { connection ->
        connection.prepareStatement(queryOppdaterPersonOppfolgingstilfelleMottatt).use {
            it.setString(1, oversikthendelsetilfelle.enhetId)
            it.setTimestamp(2, tidspunkt)
            it.setString(3, oversikthendelsetilfelle.fnr)
            it.execute()
        }
        connection.commit()
    }
    opprettEllerOppdaterOppfolgingstilfelle(personId, oversikthendelsetilfelle)
}

fun ResultSet.toPersonOppfolgingstilfelleInternal(): PersonOppfolgingstilfelleInternal =
        PersonOppfolgingstilfelleInternal(
                id = getInt("id"),
                personOversiktStatusId = getInt("person_oversikt_status_id"),
                virksomhetsnummer = getString("virksomhetsnummer"),
                fom = convert(getTimestamp("fom")),
                tom = convert(getTimestamp("tom")),
                gradert = getObject("gradert") as Boolean
        )
