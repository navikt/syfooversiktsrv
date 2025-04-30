package no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet

import java.io.Serializable

data class BehandlendeEnhetResponseDTO(
    val geografiskEnhet: Enhet,
    val oppfolgingsenhet: Enhet,
) : Serializable

data class Enhet(
    var enhetId: String,
    var navn: String
) : Serializable
