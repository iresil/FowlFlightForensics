# FowlFlightForensics
 A Kafka-based CSV processor for bird-related airplane accidents

## Description
This is a project created using **Java 22** and **Spring Boot 3.2.5**, which aims to parse a CSV file, process it within
a Kafka cluster and produce an output CSV. The input CSV file contains detailed information about aircraft accidents caused
by various wildlife species, including the type of damage that was caused and the number of creatures that caused the incident.

The original dataset contained a lot of invalid data which had to be cleaned up in order for any sort of calculations to be
possible. Since most of that information couldn't be deduced (and performing imputations wasn't part of the objective),
the following steps were taken:
1. A somewhat rough data analysis process was performed on the contents of the original CSV file, to determine which values
   could be considered valid and which not.
2. A set of rules that reflected these observations was created and added to a Map (see `validationRules` in `IncidentValidator`).
3. The contents of the input CSV were evaluated using the rules mentioned above. This was done using Java code, on
   application startup. 
4. The total amount of issues found per rule was calculated in the Java code, and compared to the total size of the dataset.
5. If the application of a specific rule returned fewer than 3% of the dataset's total entries, then the metric that the
   rule in question was validating was selected as a potential candidate to be used for statistics calculation. 

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
