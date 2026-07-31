# Makefile para despliegue y reproducción del entorno PFC-UTEQ

.PHONY: help up down restart logs ps clean test

help:
	@echo "Comandos disponibles:"
	@echo "  make up      - Levanta todo el entorno con Docker Compose"
	@echo "  make down    - Detiene y elimina contenedores"
	@echo "  make restart - Reinicia los servicios"
	@echo "  make logs    - Muestra los logs en tiempo real"
	@echo "  make ps      - Muestra el estado de los contenedores"
	@echo "  make clean   - Limpia volúmenes y contenedores huérfanos"

up:
	@echo "Levantando la infraestructura de contenedores..."
	docker compose up -d --build

down:
	@echo "Deteniendo los servicios..."
	docker compose down

restart: down up

logs:
	docker compose logs -f

ps:
	docker compose ps

clean:
	docker compose down -v --remove-orphans
