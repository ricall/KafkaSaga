SHELL=bash

.PHONY: clean start-dev stop-dev start stop db-terminal run build image

clean: stop
	@docker container ls -q --filter status=exited --filter status=created | docker rm || true

start-dev:
	@docker-compose -f docker-compose-local.yml up zookeeper kafka database

stop-dev:
	@docker-compose -f docker-compose-local.yml down

start: image
	@docker-compose up

stop:
	@docker-compose down

db-terminal:
	@docker-compose run database bash

run:
	@./gradlew bootRun

build:
	@./gradlew build

image: build
	@docker build -t kafkasaga .