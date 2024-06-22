package FowlFlightForensics.config;

import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaConfig extends BaseComponent {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;
    @Value("${app.kafka.topics.clean}")
    private String cleanDataTopic;
    @Value("${app.kafka.topics.grouped.creatures}")
    private String groupedCreaturesTopic;
    @Value("${app.kafka.topics.grouped.incidents}")
    private String groupedIncidentsTopic;
    @Value("${app.kafka.topics.grouped.top-n}")
    private String topNIncidentsTopic;
    @Value("${app.kafka.topics.invalid-species}")
    private String invalidSpeciesTopic;
    @Value("${app.kafka.topics.invalid-quantity}")
    private String invalidQuantityTopic;
    @Value("${app.kafka.topics.invalid-generic}")
    private String invalidGenericTopic;

    @Value("${app.kafka.topics.config.partitions}")
    private int partitions;
    @Value("${app.kafka.topics.config.replicas}")
    private int replicas;
    @Value("${app.kafka.topics.config.min-in-sync-replicas}")
    private Integer minInSyncReplicas;
    @Value("${app.kafka.streams.config.threads}")
    private int threads;

    @Bean
    public AdminClient generateKafkaAdminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.CLIENT_ID_CONFIG, "local-admin-1");
        return AdminClient.create(configs);
    }

    // region [Topics]
    @Bean
    public KafkaAdmin.NewTopics generateTopics() {
        return new KafkaAdmin.NewTopics(createKeyfulTopic(rawDataTopic),
                createKeyfulTopic(cleanDataTopic),
                createKeyfulTopic(groupedCreaturesTopic),
                createKeyfulTopic(groupedIncidentsTopic),
                createKeyfulTopic(topNIncidentsTopic),
                createKeyfulTopic(invalidSpeciesTopic),
                createKeyfulTopic(invalidQuantityTopic),
                createKeyfulTopic(invalidGenericTopic));
    }

    private NewTopic createKeyfulTopic(final String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                //.compact()  // Can't delete from compacted topics
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInSyncReplicas.toString())
                .build();
    }
    // endregion

    // region [Producer]
    @Primary
    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory());
        kafkaTemplate.setProducerListener(new ProducerListener<>() {
        	//@Override
        	//public void onSuccess(ProducerRecord<Object, Object> producerRecord, RecordMetadata recordMetadata) {
        	//	logger.trace("ACK received from broker for record with key {} and value {} at offset {}",
        	//				 producerRecord.key(), producerRecord.value(), recordMetadata.offset());
        	//}
            //
        	//@Override
        	//public void onError(ProducerRecord<Object, Object> producerRecord, RecordMetadata recordMetadata,
        	//					Exception exception) {
        	//	logger.warn("Unable to produce message for record with key {} and value {}.",
        	//				producerRecord.key(), producerRecord.value(), exception);
        	//}
        });

        return kafkaTemplate;
    }

    private ProducerFactory<Object, Object> producerFactory() {
        Map<String, Object> configProperties = getDefaultProducerConfig();
        return new DefaultKafkaProducerFactory<>(configProperties);
    }

    private Map<String, Object> getDefaultProducerConfig() {
        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384");
        configProperties.put(ProducerConfig.LINGER_MS_CONFIG, "0");
        configProperties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");
        configProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        configProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "data-producer");
        configProperties.put(ProducerConfig.RETRIES_CONFIG, "3");
        configProperties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, "false");

        return configProperties;
    }
    // endregion

    // region [Streams]
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig() {
        HashMap<String, Object> props = new HashMap<>();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-stream");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, JsonKeySerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonValueSerde.class);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "3000");
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, RocksDBConfig.class);
        props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, replicas);
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, replicas);
        props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, threads);
        props.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all");
        props.put(StreamsConfig.STATE_DIR_CONFIG, Paths.get(Paths.get(".").normalize().toAbsolutePath().toString(),"kafka-streams").toString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, "500");
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, "false");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        return new KafkaStreamsConfiguration(props);
    }

    //@Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
    //public StreamsBuilderFactoryBean defaultKafkaStreamsBuilder() {
    //    return new StreamsBuilderFactoryBean(kafkaStreamsConfig(), new CleanupConfig(true, true));
    //}
    // endregion

    // region [Consumer]
    @Bean("groupedListenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Object, Object>> groupedListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(groupedConsumerFactory());
        factory.setConcurrency(threads);
        factory.getContainerProperties().setIdleBetweenPolls(500);
        factory.getContainerProperties().setPollTimeout(5000);
        factory.getContainerProperties().setAckCount(10);
        factory.getContainerProperties().setAckTime(10000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.COUNT_TIME);

        return factory;
    }

    @Bean("rankedListenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Object, Object>> rankedListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(rankedConsumerFactory());
        factory.setConcurrency(threads);
        factory.getContainerProperties().setIdleBetweenPolls(500);
        factory.getContainerProperties().setPollTimeout(5000);
        factory.getContainerProperties().setAckCount(10);
        factory.getContainerProperties().setAckTime(10000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.COUNT_TIME);

        return factory;
    }

    private ConsumerFactory<Object, Object> groupedConsumerFactory() {
        Map<String, Object> configProperties = getDefaultConsumerConfig();
        configProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        configProperties.put(JsonDeserializer.KEY_DEFAULT_TYPE, "FowlFlightForensics.domain.dto.IncidentKey");
        return new DefaultKafkaConsumerFactory<>(new HashMap<>(configProperties));
    }

    private ConsumerFactory<Object, Object> rankedConsumerFactory() {
        Map<String, Object> configProperties = getDefaultConsumerConfig();
        configProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        configProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(new HashMap<>(configProperties));
    }

    private Map<String, Object> getDefaultConsumerConfig() {
        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        configProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProperties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.RoundRobinAssignor");
        configProperties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        return configProperties;
    }
    // endregion
}
