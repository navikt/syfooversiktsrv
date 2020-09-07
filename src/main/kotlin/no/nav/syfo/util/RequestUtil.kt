package no.nav.syfo.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import net.logstash.logback.argument.StructuredArguments
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    this.call.request.headers[NAV_CALL_ID_HEADER].let {
        return it ?: createCallId()
    }
}

fun createCallId(): String = UUID.randomUUID().toString()

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

private val kafkaCounter = AtomicInteger(0)

fun kafkaCallId(): String = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-HHmm"))}-syfooversiktsrv-kafka-${kafkaCounter.incrementAndGet()}"
