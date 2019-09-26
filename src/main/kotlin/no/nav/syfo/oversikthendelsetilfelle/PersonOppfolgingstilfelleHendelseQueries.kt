@file:Suppress("SqlNoDataSourceInspection")

package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.oversikthendelsetilfelle.domain.*
import no.nav.syfo.util.convert
import java.sql.*
import java.sql.Types.NULL
import java.time.Instant
import java.util.*

const val queryHentPersonsOppfolgingstilfellerGjeldendeI8UkerUtenAktivitet = """
                        SELECT *
                        FROM PERSON_OPPFOLGINGSTILFELLE
                        WHERE person_oversikt_status_id = ? AND gradert = 'f' AND fom <= now() - interval '8 week' AND tom > now() - interval '17 day'
                """

fun DatabaseInterface.hentPersonsOppfolgingstilfellerGjeldendeI8UkerUtenAktivitet(personId: Int): List<PPersonOppfolgingstilfelle> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentPersonsOppfolgingstilfellerGjeldendeI8UkerUtenAktivitet).use {
            it.setInt(1, personId)
            it.executeQuery().toList { toPPersonOppfolgingstilfelle() }
        }
    }
}

const val queryHentOppfolgingstilfelleForPerson = """
                         SELECT *
                         FROM PERSON_OPPFOLGINGSTILFELLE
                         WHERE person_oversikt_status_id = ?
                """

fun DatabaseInterface.hentOppfolgingstilfellerForPerson(personId: Int): List<PPersonOppfolgingstilfelle> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentOppfolgingstilfelleForPerson).use {
            it.setInt(1, personId)
            it.executeQuery().toList { toPPersonOppfolgingstilfelle() }
        }
    }
}

const val queryHentOppfolgingstilfelleResultat = """
                         SELECT *
                         FROM PERSON_OPPFOLGINGSTILFELLE
                         WHERE (person_oversikt_status_id = ? AND virksomhetsnummer = ?)
                """

fun DatabaseInterface.hentOppfolgingstilfelleResultat(personId: Int, virksomhetsnummer: String): List<PPersonOppfolgingstilfelle> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentOppfolgingstilfelleResultat).use {
            it.setInt(1, personId)
            it.setString(2, virksomhetsnummer)
            it.executeQuery().toList { toPPersonOppfolgingstilfelle() }
        }
    }
}

const val queryOppdaterOppfolgingstilfelleMottatt = """
                        UPDATE PERSON_OPPFOLGINGSTILFELLE
                        SET gradert = ?, fom = ?, tom = ?, sist_endret = ?, virksomhetsnavn = ?
                        WHERE person_oversikt_status_id = ?
                """

fun DatabaseInterface.oppdaterOppfolgingstilfelleMottatt(personId: Int, oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2) {
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOppdaterOppfolgingstilfelleMottatt).use {
            it.setBoolean(1, oversikthendelsetilfelleV2.gradert)
            it.setTimestamp(2, convert(oversikthendelsetilfelleV2.fom))
            it.setTimestamp(3, convert(oversikthendelsetilfelleV2.tom))
            it.setTimestamp(4, tidspunkt)
            it.setString(5, oversikthendelsetilfelleV2.virksomhetsnavn)
            it.setInt(6, personId)
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
        virksomhetsnavn,
        fom,
        tom,
        gradert,
        opprettet,
        sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""

fun DatabaseInterface.opprettOppfolgingstilfelleMottatt(personId: Int, oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOpprettOppfolgingstilfelleMottatt).use {
            it.setString(1, uuid)
            it.setInt(2, personId)
            it.setString(3, oversikthendelsetilfelleV2.virksomhetsnummer)
            it.setString(4, oversikthendelsetilfelleV2.virksomhetsnavn)
            it.setTimestamp(5, convert(oversikthendelsetilfelleV2.fom))
            it.setTimestamp(6, convert(oversikthendelsetilfelleV2.tom))
            it.setBoolean(7, oversikthendelsetilfelleV2.gradert)
            it.setTimestamp(8, tidspunkt)
            it.setTimestamp(9, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.opprettEllerOppdaterOppfolgingstilfelle(personId: Int, oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2) {
    val resultatListe = hentOppfolgingstilfelleResultat(personId, oversikthendelsetilfelleV2.virksomhetsnummer)

    if (resultatListe.isEmpty()) {
        opprettOppfolgingstilfelleMottatt(personId, oversikthendelsetilfelleV2)
    } else {
        oppdaterOppfolgingstilfelleMottatt(personId, oversikthendelsetilfelleV2)
    }
}

const val queryOpprettPersonOppfolgingstilfelleMottatt = """INSERT INTO PERSON_OVERSIKT_STATUS (
        id,
        uuid,
        fnr,
        tildelt_enhet,
        opprettet,
        sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?) RETURNING id"""

fun DatabaseInterface.opprettPersonOppfolgingstilfelleMottatt(oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    var personId: Int? = null

    connection.use { connection ->
        val personIdList = connection.prepareStatement(queryOpprettPersonOppfolgingstilfelleMottatt).use {
            it.setString(1, uuid)
            it.setString(2, oversikthendelsetilfelleV2.fnr)
            it.setString(3, oversikthendelsetilfelleV2.enhetId)
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
    personId?.let { opprettEllerOppdaterOppfolgingstilfelle(it, oversikthendelsetilfelleV2) }
}

const val queryOpprettPersonOppfolgingstilfelleNyEnhetMottatt = """
                        UPDATE PERSON_OVERSIKT_STATUS
                        SET tildelt_veileder = ?, tildelt_enhet = ?, sist_endret = ?
                        WHERE fnr = ?
                """

fun DatabaseInterface.oppdaterPersonOppfolgingstilfelleNyEnhetMottatt(personId: Int, oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2) {
    val tidspunkt = Timestamp.from(Instant.now())
    connection.use { connection ->
        connection.prepareStatement(queryOpprettPersonOppfolgingstilfelleNyEnhetMottatt).use {
            it.setNull(1, NULL)
            it.setString(2, oversikthendelsetilfelleV2.enhetId)
            it.setTimestamp(3, tidspunkt)
            it.setString(4, oversikthendelsetilfelleV2.fnr)
            it.execute()
        }
        connection.commit()
    }
    opprettEllerOppdaterOppfolgingstilfelle(personId, oversikthendelsetilfelleV2)
}

const val queryOppdaterPersonOppfolgingstilfelleMottatt = """
                        UPDATE PERSON_OVERSIKT_STATUS
                        SET tildelt_enhet = ?, sist_endret = ?
                        WHERE fnr = ?
                """

fun DatabaseInterface.oppdaterPersonOppfolgingstilfelleMottatt(personId: Int, oversikthendelsetilfelleV2: KOversikthendelsetilfelleV2) {
    val tidspunkt = Timestamp.from(Instant.now())
    connection.use { connection ->
        connection.prepareStatement(queryOppdaterPersonOppfolgingstilfelleMottatt).use {
            it.setString(1, oversikthendelsetilfelleV2.enhetId)
            it.setTimestamp(2, tidspunkt)
            it.setString(3, oversikthendelsetilfelleV2.fnr)
            it.execute()
        }
        connection.commit()
    }
    opprettEllerOppdaterOppfolgingstilfelle(personId, oversikthendelsetilfelleV2)
}

fun ResultSet.toPPersonOppfolgingstilfelle(): PPersonOppfolgingstilfelle =
        PPersonOppfolgingstilfelle(
                id = getInt("id"),
                personOversiktStatusId = getInt("person_oversikt_status_id"),
                virksomhetsnummer = getString("virksomhetsnummer"),
                fom = convert(getTimestamp("fom")),
                tom = convert(getTimestamp("tom")),
                gradert = getObject("gradert") as Boolean,
                virksomhetsnavn = getString("virksomhetsnavn")
        )
