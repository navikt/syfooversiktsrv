package no.nav.syfo.pdlpersonhendelse

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.metric.COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_UPDATES
import no.nav.syfo.pdlpersonhendelse.db.updatePersonOversiktStatusNavn
import org.slf4j.LoggerFactory

class PdlPersonhendelseService(
    private val database: DatabaseInterface,
) {
    fun handlePersonhendelse(personhendelse: Personhendelse) {
        if (personhendelse.navn != null) {
            personhendelse.personidenter.forEach { personIdent ->
                val updates = database.updatePersonOversiktStatusNavn(PersonIdent(personIdent))
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
