package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.toPersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun generatePersonOversiktStatus(
    fnr: String = UserConstants.ARBEIDSTAKER_FNR,
    enhet: String = UserConstants.NAV_ENHET,
    dialogmotekandidat: Boolean = true,
): PersonOversiktStatus =
    generatePPersonOversiktStatus(fnr).copy(
        veilederIdent = "Z999999",
        enhet = enhet,
        oppfolgingstilfelleUpdatedAt = OffsetDateTime.now(),
        oppfolgingstilfelleGeneratedAt = OffsetDateTime.now(),
        oppfolgingstilfelleStart = LocalDate.now().minusDays(14),
        oppfolgingstilfelleEnd = LocalDate.now(),
        oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
        oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
        dialogmotekandidat = dialogmotekandidat,
        dialogmotekandidatGeneratedAt = if (dialogmotekandidat) OffsetDateTime.now().minusDays(8) else null,
    ).toPersonOversiktStatus(emptyList())
