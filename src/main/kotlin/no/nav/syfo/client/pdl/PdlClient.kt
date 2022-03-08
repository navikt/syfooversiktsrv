package no.nav.syfo.client.pdl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.azuread.AzureAdToken
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class PdlClient(
    private val azureAdClient: AzureAdClient,
    private val pdlClientId: String,
    private val pdlBaseUrl: String,
    private val redisStore: RedisStore,
) {
    private val httpClient = httpClientDefault()

    suspend fun personIdentNavnMap(
        callId: String,
        personIdentList: List<PersonIdent>,
    ): Map<String, String> {
        val cachedPersonIdentNameMap = getCachedPersonidentNameMap(
            personIdentList = personIdentList,
        )

        val notCachedPersonIdentList = personIdentList.filterNot { personIdentNumber ->
            cachedPersonIdentNameMap.containsKey(personIdentNumber.value)
        }

        return if (notCachedPersonIdentList.isEmpty()) {
            cachedPersonIdentNameMap
        } else {
            val pdlPersonIdentNameMap = getPdlPersonIdentNumberNavnMap(
                callId = callId,
                personIdentList = notCachedPersonIdentList,
            )
            cachedPersonIdentNameMap + pdlPersonIdentNameMap
        }
    }

    private suspend fun getPdlPersonIdentNumberNavnMap(
        callId: String,
        personIdentList: List<PersonIdent>,
    ): Map<String, String> {
        val token = azureAdClient.getSystemToken(pdlClientId)
            ?: throw RuntimeException("Failed to send request to PDL: No token was found")

        val pdlPersonIdentNameMap = personList(
            callId = callId,
            personIdentList = personIdentList,
            token = token,
        )?.hentPersonBolk?.associate { (ident, person) ->
            ident to (person?.fullName() ?: "")
        }

        pdlPersonIdentNameMap?.let {
            setCachedPersonidentNameMap(pdlPersonIdentNameMap)
        }
        return pdlPersonIdentNameMap ?: emptyMap()
    }

    private fun getCachedPersonidentNameMap(
        personIdentList: List<PersonIdent>,
    ): Map<String, String> {
        val cachedList = redisStore.getObjectList(
            classType = PdlPersonidentNameCache::class,
            keyList = personIdentList.map { personIdentNumber ->
                personIdentNameCacheKey(personIdentNumber.value)
            },
        )
        if (cachedList.isEmpty()) {
            return emptyMap()
        }
        return cachedList.associate { pdlPersonIdentNameCache ->
            pdlPersonIdentNameCache.personIdent to (pdlPersonIdentNameCache.name)
        }
    }

    private fun setCachedPersonidentNameMap(
        personIdentNameMap: Map<String, String>,
    ) {
        personIdentNameMap.forEach { personIdentName ->
            redisStore.setObject(
                key = personIdentNameCacheKey(personIdentName.key),
                value = PdlPersonidentNameCache(
                    name = personIdentName.value,
                    personIdent = personIdentName.key,
                ),
                expireSeconds = CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS,
            )
        }
    }

    private suspend fun personList(
        callId: String,
        personIdentList: List<PersonIdent>,
        token: AzureAdToken,
    ): PdlHentPersonBolkData? {
        val query = getPdlQuery(
            queryFilePath = "/pdl/hentPersonBolk.graphql",
        )

        val request = PdlPersonBolkRequest(
            query = query,
            variables = PdlPersonBolkVariables(
                identer = personIdentList.map { personIdentNumber ->
                    personIdentNumber.value
                }
            ),
        )

        val response: HttpResponse = httpClient.post(pdlBaseUrl) {
            body = request
            header(HttpHeaders.Authorization, bearerHeader(token.accessToken))
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(TEMA_HEADER, ALLE_TEMA_HEADERVERDI)
            header(NAV_CALL_ID_HEADER, callId)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val pdlPersonReponse = response.receive<PdlPersonBolkResponse>()
                return if (!pdlPersonReponse.errors.isNullOrEmpty()) {
                    COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                    pdlPersonReponse.errors.forEach {
                        logger.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                    }
                    null
                } else {
                    COUNT_CALL_PDL_PERSONBOLK_SUCCESS.increment()
                    pdlPersonReponse.data
                }
            }
            else -> {
                COUNT_CALL_PDL_PERSONBOLK_FAIL.increment()
                logger.error("Request with url: $pdlBaseUrl failed with reponse code ${response.status.value}")
                return null
            }
        }
    }

    private fun personIdentNameCacheKey(personIdentNumber: String) =
        "$CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX$personIdentNumber"

    private fun getPdlQuery(queryFilePath: String): String {
        return this::class.java.getResource(queryFilePath)!!
            .readText()
            .replace("[\n\r]", "")
    }

    companion object {
        const val CACHE_PDL_PERSONIDENT_NAME_KEY_PREFIX = "pdl-personident-name-"
        const val CACHE_PDL_PERSONIDENT_NAME_TIME_TO_LIVE_SECONDS = 7 * 24 * 60 * 60L

        private val logger = LoggerFactory.getLogger(PdlClient::class.java)
    }
}
