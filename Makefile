GIT_REV:=$(shell git rev-parse --verify HEAD)
SUITE?=test

up:
	docker-compose -f docker-compose.yml up -d --build
down:
	docker-compose down -v --remove-orphans
