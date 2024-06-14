package no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet

import java.io.Serializable

data class BehandlendeEnhetDTO(
    var enhetId: String,
    var navn: String
) : Serializable
