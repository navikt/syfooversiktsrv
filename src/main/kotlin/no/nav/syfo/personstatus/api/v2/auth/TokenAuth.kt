package no.nav.syfo.personstatus.api.v2.auth

import com.auth0.jwt.JWT
import java.lang.Error

const val JWT_CLAIM_NAVIDENT = "NAVident"

fun getNAVIdentFromToken(token: String): String {
    val decodedJWT = JWT.decode(token)
    return decodedJWT.claims[JWT_CLAIM_NAVIDENT]?.asString()
        ?: throw Error("Missing NAVident in private claims")
}
