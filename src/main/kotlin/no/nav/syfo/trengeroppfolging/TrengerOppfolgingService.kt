package no.nav.syfo.trengeroppfolging

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.trengeroppfolging.domain.TrengerOppfolging
import no.nav.syfo.trengeroppfolging.kafka.COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.updateTrengerOppfolging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TrengerOppfolgingService(
    private val database: DatabaseInterface,
) {
    fun processTrengerOppfolging(records: List<TrengerOppfolging>) {
        database.connection.use { connection ->
            records.forEach { trengerOppfolging ->
                val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
                    fnr = trengerOppfolging.personIdent.value,
                ).firstOrNull()

                if (existingPersonOversiktStatus == null) {
                    val personoversiktStatus = trengerOppfolging.toPersonoversiktStatus()
                    connection.createPersonOversiktStatus(
                        commit = false,
                        personOversiktStatus = personoversiktStatus,
                    )
                } else {
                    connection.updateTrengerOppfolging(
                        trengerOppfolging = trengerOppfolging.isActive,
                        personIdent = trengerOppfolging.personIdent,
                    )
                }

                log.info("Received trengerOppfolging with uuid=${trengerOppfolging.uuid}")
                COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ.increment()
            }
            connection.commit()
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
