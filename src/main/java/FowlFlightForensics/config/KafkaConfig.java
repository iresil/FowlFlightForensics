package FowlFlightForensics.config;

import FowlFlightForensics.util.BaseComponent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig extends BaseComponent {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;
    @Value("${app.kafka.topics.grouped}")
    private String groupedDataTopic;
    @Value("${app.kafka.topics.invalid-species}")
    private String invalidSpeciesTopic;
    @Value("${app.kafka.topics.invalid-quantity}")
    private String invalidQuantityTopic;

    @Bean
    public AdminClient generateKafkaAdminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.CLIENT_ID_CONFIG, "local-admin-1");
        return AdminClient.create(configs);
    }

    @Primary
    @Bean
    public KafkaTemplate<Long, Object> kafkaTemplate() {
        var kafkaTemplate = new KafkaTemplate<>(producerFactory(false));
        //		kafkaTemplate.setProducerListener(new ProducerListener<>() {
        //			@Override
        //			public void onSuccess(ProducerRecord<Long, Object> producerRecord, RecordMetadata recordMetadata) {
        //				logger.trace("ACK received from broker for record with key {} and value {} at offset {}",
        //							 producerRecord.key(), producerRecord.value(), recordMetadata.offset());
        //			}
        //
        //			@Override
        //			public void onError(ProducerRecord<Long, Object> producerRecord, RecordMetadata recordMetadata,
        //								Exception exception) {
        //				logger.warn("Unable to produce message for record with key {} and value {}.",
        //							producerRecord.key(), producerRecord.value(), exception);
        //			}
        //		});

        return kafkaTemplate;
    }

    private ProducerFactory<Long, Object> producerFactory(final boolean transactional) {
        Map<String, Object> configProperties = getDefaultConfigurationProperties();
        if (transactional) {
            configProperties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "X1-");
            configProperties.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 60000);
        }
        return new DefaultKafkaProducerFactory<>(configProperties);
    }

    private Map<String, Object> getDefaultConfigurationProperties() {
        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384");
        configProperties.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        configProperties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");
        configProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        configProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "data-producer");
        configProperties.put(ProducerConfig.RETRIES_CONFIG, "3");
        configProperties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, "true");

        return configProperties;
    }

    @Bean
    public KafkaAdmin.NewTopics generateTopics() {
        return new KafkaAdmin.NewTopics(createKeyfulTopic(rawDataTopic),
                createKeyfulTopic(groupedDataTopic),
                createKeyfulTopic(invalidSpeciesTopic),
                createKeyfulTopic(invalidQuantityTopic));
    }

    private NewTopic createKeyfulTopic(final String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(2)
                .replicas(1)
                //.compact()  // Can't delete from compacted topics
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }
}
