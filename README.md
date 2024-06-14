# FowlFlightForensics
A Kafka-based CSV processor for bird-related airplane accidents

> [!WARNING]
> This project was created with the purpose of exploring various different scenarios related to Kafka. It should not be
> considered production-level by any means. Many parts of the code have been purposefully put together in a way that is
> very far from "the Kafka way", in order to see what would happen.

## Description
This is a project created using **Java 22** and **Spring Boot 3.2.5**, which aims to parse a CSV file, process it within
a Kafka cluster and produce an output CSV. The input CSV file contains detailed information about aircraft accidents caused
by various wildlife species, including the type of damage that was caused and the number of creatures that caused the incident.

### Data Preparation
The original dataset contained a lot of invalid data which had to be cleaned up in order for any sort of calculations to be
possible. Since most of that information couldn't be deduced (and performing imputations wasn't part of the objective),
the following steps were taken:
1. A somewhat rough data analysis process was performed on the contents of the original CSV file, to determine which values
   could be considered valid and which not.
2. A set of rules that reflected these observations was created and added to a Map (see `validationRules` in `IncidentValidator`).
3. The contents of the input CSV were evaluated using the rules mentioned above. This was done using Java code, on
   application startup. 
4. The total amount of issues found per rule was calculated in the Java code, and compared to the total size of the dataset.
5. If the application of a specific rule identified fewer than 3% of the dataset's total entries as invalid, then the metric
   that the rule in question was validating was selected as "valid enough" to be a potential candidate to be used for statistics
   calculation.
6. The metrics that were selected for statistics calculation are the same metrics on which validations will be applied
   (to ensure that the end results will eventually make sense). The rest of the metrics will simply be ignored.

### Data Pipeline
In general, what happens to the data during each execution is pretty straightforward:
1. The CSV file gets loaded and parsed into a List of `IncidentDetails` objects.
2. The objects are validated based on a predefined set of rules and the total percentage of invalid objects is calculated
   based on each rule.
3. Rules with an invalid object frequency under 3% are selected to be applied later.
4. The `IncidentDetails` objects are transformed to `IncidentSummary` objects, by removing most of their fields and keeping
   only fields of interest.
5. Each object is sent to the `raw-data-topic`, using a Kafka **Producer**. Each message is formatted as a pair of `IncidentKey`
   and `IncidentSummary`.
6. The selected rules from step **(3)** are applied to all messages in that topic, using **KStreams**. Invalid messages are
   forwarded to either `invalid-species-topic`, `invalid-quantity-topic` or `invalid-generic-topic`.
7. The messages that are still considered valid, after the application of the selected rules, are grouped and aggregated
   in two separate ways, using **KStreams**:
   - By calculating the sum of average _creature_ counts, which provides a rough estimation of how many creatures in total
     caused aircraft accidents per **year**, **month** and **species**. The results of this calculation are sent to `grouped-creatures-topic`.
   - By calculating the total number of _incidents_, which describes how many incidents each **species** caused per **year** and
     **month** combination. The results of this calculation are sent to `grouped-incidents-topic`.
8. Grouped incidents are then received by a separate Kafka **Consumer**, and the **top 5 species** that caused the most incidents
   per **year** and **month** are selected.
9. The results are exported to a CSV file.

### Caveats
To make the application's execution easier, certain choices were made that don't align with Kafka best practices. These choices
were made consciously, since the project itself is more of an exploration into what Kafka has to offer, instead of actual
production-level code. Some examples of this are the following:
- Each new execution of the code runs from scratch, without reusing any results of previous executions. To achieve this,
  the following things are done:
  - The local state store (the folder of which has been conveniently moved under the project's root directory) gets deleted
    on application start up, by `DataWiperService`.
  - All connected topics and partitions are completely emptied of messages (on application start up, by `DataWiperService`).
  - Each time aggregations are performed and the state store is necessary, a new store name is generated using a UUID, so
    that aggregations from previous runs aren't taken into account. This was done because there were permissions issues
    when attempting to delete data from the state store.
- The entire pipeline is packaged in one single application. In a production environment, this code would probably be built
  as a set of microservices, so that the appropriate resources can be dedicated to each part of the stream.
- The stream to process gets created from a CSV file and the output gets written to a CSV file. Both files always have the
  same contents. This doesn't make much sense from a Kafka perspective, since in a real-case scenario we would have a steady
  stream of information being processed.

## Environment Setup
### Requirements
To successfully set up and run the project on a Windows machine, you first need the following things installed:
- [WSL/WSL2](https://learn.microsoft.com/en-us/windows/wsl/install)
- [Docker Desktop](https://docs.docker.com/desktop/install/windows-install/)

### Instructions
- In Docker Desktop, search for **apache/kafka** and pull version [3.7.0](https://hub.docker.com/layers/apache/kafka/3.7.0/images/sha256-3e324d2bd331570676436b24f625e5dcf1facdfbd62efcffabc6b69b1abc13cc).
- **Run** the image to create a container named ApacheKafka.
- **Start** the container and execute the following commands in its terminal to retrieve its cluster id:
  ```bash
  cd opt/kafka/bin
  ./kafka-metadata-quorum.sh --bootstrap-server localhost:9092 describe --status
  ```
- Open **PowerShell**, go to the **docker** folder found in your project directory, and execute the following command
  to create a new docker image, based on **apache/kafka:3.7.0** (using the cluster id from the previous result):
  ```bash
  docker build --build-arg CLUSTER_ID=5L6g3nShT-eMCtK--X86sw -t kraft-kafka -f Dockerfile .
  ```
- Run **docker-compose.yml** from the IntelliJ UI to create and start the containers that will host your topics.

### Executing Manual Commands
You can execute manual commands from outside your newly created cluster.
To do this, you can execute the following commands in the WSL terminal:
- To install Java 21:
  ```bash
  sudo apt install openjdk-21-jdk
  ```
- To download and unzip KRaft Kafka:
  ```bash
  wget https://archive.apache.org/dist/kafka/3.7.0/kafka_2.13-3.7.0.tgz
  tar -xvzf kafka_2.13-3.7.0.tgz
  ```
- To use the shell scripts included in the Kafka installation (e.g. to list available cluster members, list all topics,
  start a consumer on a specific topic, or delete a topic):
  ```bash
  cd kafka_2.13-3.7.0/bin
  ./kafka-broker-api-versions.sh --bootstrap-server localhost:19092 | awk '/id/{print $1}' | sort
  ```
  ```bash
  cd kafka_2.13-3.7.0/bin
  ./kafka-topics.sh --list --bootstrap-server localhost:19092, localhost:29092
  ```
  ```bash
  cd kafka_2.13-3.7.0/bin
  ./kafka-console-consumer.sh --bootstrap-server localhost:19092, localhost:29092 --topic raw-data-topic --from-beginning
  ```
  ```bash
  cd kafka_2.13-3.7.0/bin
  ./kafka-topics.sh --bootstrap-server localhost:19092 --topic raw-data-topic --delete
  ```
