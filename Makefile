# Makefile para despliegue, verificación y reproducción del entorno PFC-UTEQ

.PHONY: help up down restart logs ps clean all test bench audit docs

help:
	@echo "Comandos disponibles:"
	@echo "  make up      - Levanta todo el entorno con Docker Compose"
	@echo "  make down    - Detiene y elimina contenedores"
	@echo "  make restart - Reinicia los servicios"
	@echo "  make logs    - Muestra los logs en tiempo real"
	@echo "  make ps      - Muestra el estado de los contenedores"
	@echo "  make clean   - Limpia volúmenes y contenedores huérfanos"
	@echo "  make all     - Compila el backend y construye el build de producción del frontend"
	@echo "  make test    - Corre la suite de tests del backend (JaCoCo incluido)"
	@echo "  make bench   - Corre las 5 pruebas de carga k6 contra localhost:8080"
	@echo "  make audit   - Corre SpotBugs/find-sec-bugs (SQL dinámico) + npm audit"
	@echo "  make docs    - Regenera las figuras de docs/mediciones/perf/figuras/ y valida la matriz de trazabilidad"

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

## --- Objetivos exigidos por la guía (Fase 6) ---

all:
	@echo "Compilando el backend..."
	cd backend && ./mvnw -q compile
	@echo "Construyendo el build de producción del frontend..."
	cd Frontend && npx ng build --configuration production

test:
	@echo "Corriendo la suite de tests del backend (JaCoCo se genera en la fase test)..."
	cd backend && ./mvnw test

bench:
	@echo "Corriendo las 5 pruebas de carga k6 contra localhost:8080/api/v1..."
	@echo "Requiere el backend corriendo (make up, o mvnw spring-boot:run) y k6 instalado."
	cd k6 && k6 run load-test.js

audit:
	@echo "Análisis estático de seguridad (SpotBugs + find-sec-bugs, incluye SQL dinámico)..."
	./scripts/audit-sql-dynamic.sh
	@echo ""
	@echo "npm audit del frontend..."
	cd Frontend && npm audit || true

docs:
	@echo "Regenerando figuras de rendimiento (docs/mediciones/perf/figuras/)..."
	python scripts/gen-figuras.py
	@echo "Validando la matriz de trazabilidad contra el repositorio real..."
	./scripts/validate-traceability.sh || true
