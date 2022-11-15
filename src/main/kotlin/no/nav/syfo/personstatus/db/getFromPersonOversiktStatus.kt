package no.nav.syfo.personstatus.db

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.personstatus.domain.*
import java.sql.Connection
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

const val KNYTNING_IKKE_FUNNET = 0L

const val querygetPersonOversiktStatusList =
    """
    SELECT *
    FROM PERSON_OVERSIKT_STATUS
    WHERE fnr = ?
    """

fun Connection.getPersonOversiktStatusList(
    fnr: String,
): List<PPersonOversiktStatus> =
    this.prepareStatement(querygetPersonOversiktStatusList).use {
        it.setString(1, fnr)
        it.executeQuery().toList { toPPersonOversiktStatus() }
    }

fun DatabaseInterface.getPersonOversiktStatusList(
    fnr: String,
): List<PPersonOversiktStatus> {
    return connection.use { connection ->
        connection.getPersonOversiktStatusList(fnr = fnr)
    }
}

const val queryHentUbehandledePersonerTilknyttetEnhet = """
                        SELECT *
                        FROM PERSON_OVERSIKT_STATUS
                        WHERE ((tildelt_enhet = ?)
                            AND (motebehov_ubehandlet = 't' 
                            OR oppfolgingsplan_lps_bistand_ubehandlet = 't' 
                            OR dialogmotesvar_ubehandlet = 't' 
                            OR (
                                dialogmotekandidat = 't' 
                                AND dialogmotekandidat_generated_at + INTERVAL '7 DAY' < now()
                                )
                            )
                        );
                """

fun DatabaseInterface.hentUbehandledePersonerTilknyttetEnhet(enhet: String): List<PPersonOversiktStatus> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentUbehandledePersonerTilknyttetEnhet).use {
            it.setString(1, enhet)
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
}

fun DatabaseInterface.hentBrukereTilknyttetVeileder(veileder: String): List<VeilederBrukerKnytning> {
    val query = """
                        SELECT *
                        FROM PERSON_OVERSIKT_STATUS
                        WHERE tildelt_veileder = ?
                """
    return connection.use { connection ->
        connection.prepareStatement(query).use {
            it.setString(1, veileder)
            it.executeQuery().toList { toVeilederBrukerKnytning() }
        }
    }
}

fun ResultSet.toPPersonOversiktStatus(): PPersonOversiktStatus =
    PPersonOversiktStatus(
        id = getInt("id"),
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        navn = getString("navn"),
        enhet = getString("tildelt_enhet"),
        tildeltEnhetUpdatedAt = getObject("tildelt_enhet_updated_at", OffsetDateTime::class.java),
        motebehovUbehandlet = getObject("motebehov_ubehandlet") as Boolean?,
        oppfolgingsplanLPSBistandUbehandlet = getObject("oppfolgingsplan_lps_bistand_ubehandlet") as Boolean?,
        dialogmotesvarUbehandlet = getObject("dialogmotesvar_ubehandlet") as Boolean,
        dialogmotekandidat = getObject("dialogmotekandidat") as Boolean?,
        dialogmotekandidatGeneratedAt = getObject("dialogmotekandidat_generated_at", OffsetDateTime::class.java),
        motestatus = getString("motestatus"),
        motestatusGeneratedAt = getObject("motestatus_generated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleUpdatedAt = getObject("oppfolgingstilfelle_updated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleGeneratedAt = getObject("oppfolgingstilfelle_generated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleStart = getObject("oppfolgingstilfelle_start", LocalDate::class.java),
        oppfolgingstilfelleEnd = getObject("oppfolgingstilfelle_end", LocalDate::class.java),
        oppfolgingstilfelleBitReferanseUuid = getString("oppfolgingstilfelle_bit_referanse_uuid")?.let {
            UUID.fromString(
                it
            )
        },
        oppfolgingstilfelleBitReferanseInntruffet = getObject(
            "oppfolgingstilfelle_bit_referanse_inntruffet",
            OffsetDateTime::class.java
        ),
    )

fun ResultSet.toVeilederBrukerKnytning(): VeilederBrukerKnytning =
    VeilederBrukerKnytning(
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        enhet = getString("tildelt_enhet") ?: ""
    )
