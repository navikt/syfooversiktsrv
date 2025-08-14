package no.nav.syfo.personstatus.application

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_UPDATES
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusNavnToNull
import org.slf4j.LoggerFactory

class PdlPersonhendelseService(
    private val database: DatabaseInterface,
) {
    fun handlePersonhendelse(personhendelse: Personhendelse) {
        if (personhendelse.navn != null) {
            personhendelse.personidenter
                .mapNotNull { personIdent ->
                    try {
                        if (personIdent.isNullOrEmpty() || personIdent.length == 13) {
                            null
                        } else {
                            PersonIdent(personIdent)
                        }
                    } catch (ex: IllegalArgumentException) {
                        log.warn("Invalid personident for Personhendelse", ex)
                        null
                    }
                }
                .forEach { personIdent ->
                    val updates = database.updatePersonOversiktStatusNavnToNull(personIdent)
                    if (updates > 0) {
                        log.info("Personhendelse: Endring av navn på person vi har i databasen, navn satt til NULL")
                        COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_UPDATES.increment()
                    }
                }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PdlPersonhendelseService::class.java)
    }
}
