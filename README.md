# distributed_project

Launch one of the launchers:

```bash
./gradlew -P MAIN_CLASS=ds.MAIN_CLASS run [--args="default_physical line_virtual"]
```
, where `MAIN_CLASS` is a launcher classpath, for now the only option is `launchers.SimpleLauncher`.

Before running any launcher, RabbitMQ running is required, you can run it e.g. with this command:

```bash
docker run -it --rm --name Rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.11-management
```

docker build -f docker/Dockerfile --rm -t getting-started .

## TODOs

1. Read file IO.
