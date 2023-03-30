.ONESHELL:
.EXPORT_ALL_VARIABLES:

MAIN_CLASS=ds.Launcher
BASE_NAME=distributed_project
VERSION=0.1


rabbitmq:
	@
	docker-compose -f ./docker/docker-compose.yml up --no-recreate rabbitmq


run-container:
	@
	export RABBIT_HOST=ds-rabbitmq
	docker-compose -f ./docker/docker-compose.yml up --build

run-threading:
	@
	echo "Under construction..."

run-launcher:
	@
	docker container exec -it ds-rabbitmq rabbitmq-diagnostics -q check_running 2> /dev/null || echo "RabbitMQ is not running on localhost!"
	export RABBIT_HOST=localhost
	./gradlew -P MAIN_CLASS=$(MAIN_CLASS) -P BASE_NAME=$(BASE_NAME) -P VERSION=$(VERSION) run
