# distributed_project

Launch one of the launchers:

```bash
./gradlew -P mainClass=ds.MAIN_CLASS run
```
, where `MAIN_CLASS` is a launcher classpath, for now the only option is `launchers.Launcher`.

Before running any launcher, RabbitMQ running is required, you can run it e.g. with this command:

```bash
docker run -it --rm --name Rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.11-management
```

## TODOs

1. In-code TODOs.
2. Other tests (e.g. multithreading, Docker, other configurations, etc.).
3. Consider using (or deleting!) files from `misc` directory AND dependencies.
