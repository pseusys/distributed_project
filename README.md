# distributed_project

## Build and Run

1. Run launcher (legacy):
    `make run-launcher`  
2. Run multithreading (locally):
    `make-threading`
3. Run docker (bundled):
    `make-container`

## Requirements

For `launcher` and `multithreading`:

1. Gradle 7.6  
   Can be installed with [this installation guide](https://gradle.org/install/#with-the-gradle-wrapper).  
   The command will be `./gradlew wrapper --gradle-version=7.6 --distribution-type=bin`.  
   Installation can be verified with `./gradlew --version`.
2. JDK 11+  
   Can be installed with [this installation guide](https://openjdk.org/install/).  
   Installation can be verified with `java --version`.
3. RabbitMQ  
   Can be launched with `make rabbitmq`.
   Run can be verified with `docker container exec -it ds-rabbitmq rabbitmq-diagnostics -q check_running`.

Build and install dependencies using:

```shell
./gradlew build
```

For `docker`:

1. Docker
2. docker-compose > 2.0
