package no.nav.syfo.huskelapp

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.huskelapp.domain.Huskelapp
import no.nav.syfo.huskelapp.kafka.COUNT_KAFKA_CONSUMER_HUSKELAPP_READ
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.updateHuskelappActive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HuskelappService(
    private val database: DatabaseInterface,
) {
    fun processHuskelapp(records: List<Huskelapp>) {
        database.connection.use { connection ->
            records.forEach { huskelapp ->
                val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
                    fnr = huskelapp.personIdent.value,
                ).firstOrNull()

                if (existingPersonOversiktStatus == null) {
                    val personoversiktStatus = huskelapp.toPersonoversiktStatus()
                    connection.createPersonOversiktStatus(
                        commit = false,
                        personOversiktStatus = personoversiktStatus,
                    )
                } else {
                    connection.updateHuskelappActive(
                        isHuskelappActive = huskelapp.isActive,
                        personIdent = huskelapp.personIdent,
                    )
                }

                log.info("Received huskelapp with uuid=${huskelapp.uuid}")
                COUNT_KAFKA_CONSUMER_HUSKELAPP_READ.increment()
            }
            connection.commit()
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
