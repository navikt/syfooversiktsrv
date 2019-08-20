package no.nav.syfo.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.*

const val NAV_CALL_ID_HEADER = "X-Nav-CallId"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.request.headers[NAV_CALL_ID_HEADER].toString()
}

fun CallIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

private val kafkaCounter = AtomicInteger(0)

fun kafkaCallId(): String
        = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-HHmm"))}-syfooversiktsrv-kafka-${kafkaCounter.incrementAndGet()}"

