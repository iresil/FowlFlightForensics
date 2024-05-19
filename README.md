# FowlFlightForensics
 A Kafka-based CSV processor for bird-related airplane accidents

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
- To use the shell scripts included in the Kafka installation (e.g. to list available cluster members):
  ```bash
  cd kafka_2.13-3.7.0/bin
  ./kafka-broker-api-versions.sh --bootstrap-server localhost:19092 | awk '/id/{print $1}' | sort
  ```