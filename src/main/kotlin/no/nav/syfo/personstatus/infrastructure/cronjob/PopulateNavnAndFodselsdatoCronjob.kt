package no.nav.syfo.personstatus.infrastructure.cronjob

import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import org.slf4j.LoggerFactory

class PopulateNavnAndFodselsdatoCronjob(
    private val personoversiktStatusService: PersonoversiktStatusService,
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
) : Cronjob {
    override val initialDelayMinutes: Long = 10
    override val intervalDelayMinutes: Long = 60

    override suspend fun run() {
        log.info("Run PopulateNavnAndFodselsdatoCronjob")
        val token = azureAdClient.getSystemToken(clientEnvironment.clientId)
            ?: throw RuntimeException("Failed to send request to PDL: No token was found")
        personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(systemToken = token)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonBehandlendeEnhetCronjob::class.java)
    }
}
