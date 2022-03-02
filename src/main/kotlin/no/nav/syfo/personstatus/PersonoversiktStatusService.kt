package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.oversikthendelsetilfelle.domain.toOppfolgingstilfelle
import no.nav.syfo.oversikthendelsetilfelle.hentOppfolgingstilfellerForPerson
import no.nav.syfo.personstatus.domain.*

class PersonoversiktStatusService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {
    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String): List<PersonOversiktStatus> {
        val personListe = database.hentUbehandledePersonerTilknyttetEnhet(enhet)
        return personListe.map { pPersonOversikStatus ->
            val pOppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(pPersonOversikStatus.id)
            val oppfolgingstilfeller: List<Oppfolgingstilfelle> = pOppfolgingstilfeller.map { pOppfolgingstilfelle ->
                pOppfolgingstilfelle.toOppfolgingstilfelle()
            }
            pPersonOversikStatus.toPersonOversiktStatus(oppfolgingstilfeller = oppfolgingstilfeller)
        }.filter { pPersonOversikStatus ->
            pPersonOversikStatus.oppfolgingsplanLPSBistandUbehandlet == true || pPersonOversikStatus.oppfolgingstilfeller.isNotEmpty()
        }
    }

    suspend fun getPersonOversiktStatusListWithName(
        callId: String,
        personOversiktStatusList: List<PersonOversiktStatus>,
    ): List<PersonOversiktStatus> {
        val personIdentMissingNameList = personOversiktStatusList.filter { personOversiktStatus ->
            personOversiktStatus.navn.isNullOrEmpty()
        }.map { personOversiktStatus ->
            PersonIdent(personOversiktStatus.fnr)
        }
        return pdlClient.personIdentNavnMap(
            callId = callId,
            personIdentList = personIdentMissingNameList,
        ).let { personIdentNameMap ->
            personOversiktStatusList.addPersonName(
                personIdentNameMap = personIdentNameMap,
            )
        }
    }
}
