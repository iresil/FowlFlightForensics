FROM apache/kafka:3.7.0
ARG CLUSTER_ID
ENV CLUSTER_ID ${CLUSTER_ID}

USER root

RUN mkdir -p /data/kafka
COPY start-kafka.sh /usr/bin

RUN chmod +x /usr/bin/start-kafka.sh
CMD ["/usr/bin/start-kafka.sh", "${CLUSTER_ID}"]
