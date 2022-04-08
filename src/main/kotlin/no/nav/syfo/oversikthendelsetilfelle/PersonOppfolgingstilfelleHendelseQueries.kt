@file:Suppress("SqlNoDataSourceInspection")

package no.nav.syfo.oversikthendelsetilfelle

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.PPersonOppfolgingstilfelle
import no.nav.syfo.util.convert
import java.sql.*
import java.time.Instant
import java.util.*

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

fun DatabaseInterface.hentOppfolgingstilfelleResultat(
    personId: Int,
    virksomhetsnummer: String
): List<PPersonOppfolgingstilfelle> {
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

fun DatabaseInterface.oppdaterOppfolgingstilfelleMottatt(
    personId: Int,
    oversikthendelsetilfelle: KOversikthendelsetilfelle
) {
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOppdaterOppfolgingstilfelleMottatt).use {
            it.setBoolean(1, oversikthendelsetilfelle.gradert)
            it.setTimestamp(2, convert(oversikthendelsetilfelle.fom))
            it.setTimestamp(3, convert(oversikthendelsetilfelle.tom))
            it.setTimestamp(4, tidspunkt)
            it.setString(5, oversikthendelsetilfelle.virksomhetsnavn)
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

fun DatabaseInterface.opprettOppfolgingstilfelleMottatt(
    personId: Int,
    oversikthendelsetilfelle: KOversikthendelsetilfelle
) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    connection.use { connection ->
        connection.prepareStatement(queryOpprettOppfolgingstilfelleMottatt).use {
            it.setString(1, uuid)
            it.setInt(2, personId)
            it.setString(3, oversikthendelsetilfelle.virksomhetsnummer)
            it.setString(4, oversikthendelsetilfelle.virksomhetsnavn)
            it.setTimestamp(5, convert(oversikthendelsetilfelle.fom))
            it.setTimestamp(6, convert(oversikthendelsetilfelle.tom))
            it.setBoolean(7, oversikthendelsetilfelle.gradert)
            it.setTimestamp(8, tidspunkt)
            it.setTimestamp(9, tidspunkt)
            it.execute()
        }
        connection.commit()
    }
}

fun DatabaseInterface.opprettEllerOppdaterOppfolgingstilfelle(
    personId: Int,
    oversikthendelsetilfelle: KOversikthendelsetilfelle
) {
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
        opprettet,
        sist_endret) VALUES (DEFAULT, ?, ?, ?, ?) RETURNING id"""

fun DatabaseInterface.opprettPersonOppfolgingstilfelleMottatt(oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    val uuid = UUID.randomUUID().toString()
    val tidspunkt = Timestamp.from(Instant.now())

    var personId: Int? = null

    connection.use { connection ->
        val personIdList = connection.prepareStatement(queryOpprettPersonOppfolgingstilfelleMottatt).use {
            it.setString(1, uuid)
            it.setString(2, oversikthendelsetilfelle.fnr)
            it.setTimestamp(3, tidspunkt)
            it.setTimestamp(4, tidspunkt)
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

fun ResultSet.toPPersonOppfolgingstilfelle(): PPersonOppfolgingstilfelle =
    PPersonOppfolgingstilfelle(
        id = getInt("id"),
        sistEndret = getTimestamp("sist_endret").toLocalDateTime(),
        personOversiktStatusId = getInt("person_oversikt_status_id"),
        virksomhetsnummer = getString("virksomhetsnummer"),
        fom = convert(getTimestamp("fom")),
        tom = convert(getTimestamp("tom")),
        gradert = getObject("gradert") as Boolean,
        virksomhetsnavn = getString("virksomhetsnavn")
    )
