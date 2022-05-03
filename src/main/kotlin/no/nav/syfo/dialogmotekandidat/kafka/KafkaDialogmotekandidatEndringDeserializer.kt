package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.common.serialization.Deserializer

class KafkaDialogmotekandidatEndringDeserializer : Deserializer<KafkaDialogmotekandidatEndring> {

    private val mapper = configuredJacksonMapper()

    override fun deserialize(topic: String, data: ByteArray): KafkaDialogmotekandidatEndring =
        mapper.readValue(data, KafkaDialogmotekandidatEndring::class.java)
}
