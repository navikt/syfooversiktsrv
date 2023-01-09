package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.db.hentUbehandledePersonerTilknyttetEnhet
import no.nav.syfo.personstatus.db.updatePersonOversiktStatusNavn
import no.nav.syfo.personstatus.domain.*

class PersonoversiktStatusService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String): List<PersonOversiktStatus> {
        val personListe = database.hentUbehandledePersonerTilknyttetEnhet(enhet)
        return personListe.map { pPersonOversikStatus ->
            val personOppfolgingstilfelleVirksomhetList = getPersonOppfolgingstilfelleVirksomhetList(
                pPersonOversikStatusId = pPersonOversikStatus.id,
            )
            pPersonOversikStatus.toPersonOversiktStatus(
                personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelleVirksomhetList,
            )
        }.filter { personOversiktStatus ->
            personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet == true ||
                personOversiktStatus.dialogmotesvarUbehandlet == true ||
                personOversiktStatus.isDialogmotekandidat() ||
                (personOversiktStatus.motebehovUbehandlet == true && personOversiktStatus.latestOppfolgingstilfelle != null) ||
                personOversiktStatus.isActiveAktivitetskrav()
        }
    }

    fun getPersonOppfolgingstilfelleVirksomhetList(
        pPersonOversikStatusId: Int,
    ): List<PersonOppfolgingstilfelleVirksomhet> =
        database.connection.use { connection ->
            connection.getPersonOppfolgingstilfelleVirksomhetList(
                pPersonOversikStatusId = pPersonOversikStatusId,
            ).toPersonOppfolgingstilfelleVirksomhet()
        }

    suspend fun getPersonOversiktStatusListWithName(
        callId: String,
        personOversiktStatusList: List<PersonOversiktStatus>,
    ): List<PersonOversiktStatus> {
        val personIdentMissingNameList = personOversiktStatusList
            .filter { it.navn.isNullOrEmpty() }
            .map { personOversiktStatus ->
                PersonIdent(personOversiktStatus.fnr)
            }

        return if (personIdentMissingNameList.isEmpty()) {
            personOversiktStatusList
        } else {
            val personIdentNavnMap = pdlClient.getPdlPersonIdentNumberNavnMap(
                callId = callId,
                personIdentList = personIdentMissingNameList,
            )
            database.updatePersonOversiktStatusNavn(personIdentNavnMap)
            personOversiktStatusList.addPersonName(personIdentNameMap = personIdentNavnMap)
        }
    }
}
