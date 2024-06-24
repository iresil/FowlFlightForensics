package FowlFlightForensics;

import FowlFlightForensics.domain.dto.IncidentKey;
import FowlFlightForensics.domain.dto.IncidentSummary;
import FowlFlightForensics.service.DataWiperService;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RunAs;
import static org.assertj.core.api.Assertions.assertThat;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunAs("KafkaTests")
@SpringBootTest
@EmbeddedKafka(topics = { "raw-data-topic-test" })
public class KafkaTests {
    private static final String TEST_TOPIC = "raw-data-topic-test";

    private JsonKeySerde keySerde = new JsonKeySerde();
    private JsonValueSerde incidentSerde = new JsonValueSerde();

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    public void testKafkaIntegration() {
        Consumer<IncidentKey, IncidentSummary> consumer = configureConsumer();
        Producer<IncidentKey, IncidentSummary> producer = configureProducer();

        IncidentSummary incidentSummary = new IncidentSummary(111, 2024, 6, 24, "T-38A",
                null, null, "KIWA", "PHOENIX-MESA GATEWAY", "AZ", "AWP",
                null, "CLIMB", "K5105", "MERLIN", 2, 10,
                null, null, false);
        producer.send(new ProducerRecord<>(TEST_TOPIC, incidentSummary.getKey(), incidentSummary));

        ConsumerRecord<IncidentKey, IncidentSummary> singleRecord = KafkaTestUtils.getSingleRecord(consumer, TEST_TOPIC);
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.key()).isEqualTo(incidentSummary.getKey());
        assertThat(singleRecord.value()).isEqualTo(incidentSummary);

        consumer.close();
        producer.close();
    }

    private Consumer<IncidentKey, IncidentSummary> configureConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<IncidentKey, IncidentSummary> consumer = new DefaultKafkaConsumerFactory<IncidentKey, IncidentSummary>(consumerProps,
                keySerde.deserializer(), incidentSerde.deserializer())
                .createConsumer();
        consumer.subscribe(Collections.singleton(TEST_TOPIC));
        return consumer;
    }

    private Producer<IncidentKey, IncidentSummary> configureProducer() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        return new DefaultKafkaProducerFactory<IncidentKey, IncidentSummary>(producerProps, keySerde.serializer(),
                incidentSerde.serializer()).createProducer();
    }
}
