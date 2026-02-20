package no.nav.syfo.infrastructure.cronjob.preloadcache

import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.toList

const val queryGetEnheter =
    """
    SELECT DISTINCT tildelt_enhet
    FROM person_oversikt_status
    WHERE tildelt_enhet IS NOT NULL;
    """

fun DatabaseInterface.getEnheter(): List<String> =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetEnheter).use {
            it.executeQuery().toList { getString("tildelt_enhet") }
        }
    }
