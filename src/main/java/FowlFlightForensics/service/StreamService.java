package FowlFlightForensics.service;

import FowlFlightForensics.domain.IncidentContainer;
import FowlFlightForensics.domain.IncidentKey;
import FowlFlightForensics.domain.IncidentSummary;
import FowlFlightForensics.enums.InvalidIncidentTopic;
import FowlFlightForensics.util.BaseComponent;
import FowlFlightForensics.util.serdes.JsonKeySerde;
import FowlFlightForensics.util.serdes.JsonValueSerde;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamService extends BaseComponent {
    @Value("${app.kafka.topics.raw}")
    private String rawDataTopic;

    private final IncidentContainer incidentContainer = IncidentContainer.INSTANCE.getInstance();

    @Bean
    public KStream<IncidentKey, IncidentSummary> streamFilter(StreamsBuilder builder) {
        JsonKeySerde keySerde = new JsonKeySerde();
        JsonValueSerde incidentSerde = new JsonValueSerde();

        KStream<IncidentKey, IncidentSummary> invalidStream = builder.stream(rawDataTopic, Consumed.with(keySerde, incidentSerde));
        invalidStream.split()
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).size() == 3,
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES)
                        && incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY),
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES)
                                && incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER),
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY)
                                && incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER),
                        Branched.withConsumer(kstream -> {
                            kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue());
                            kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue());
                        }))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES),
                        Branched.withConsumer(kstream -> kstream.to(InvalidIncidentTopic.SPECIES.getAnnotationValue())))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.QUANTITY),
                        Branched.withConsumer(kstream -> kstream.to(InvalidIncidentTopic.QUANTITY.getAnnotationValue())))
                .branch((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.OTHER),
                        Branched.withConsumer(kstream -> kstream.to(InvalidIncidentTopic.OTHER.getAnnotationValue())))
                .noDefaultBranch();

        //KStream<IncidentKey, IncidentSummary> speciesInvalidOut = invalidStream
        //        .filter((k, v) -> incidentContainer.validateIncidentSummary(v).contains(InvalidIncidentTopic.SPECIES));
        //speciesInvalidOut.print(Printed.toSysOut());

        return invalidStream;
    }
}
