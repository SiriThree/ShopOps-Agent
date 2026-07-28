# Deploy

## Portfolio demo

The demo image contains the Spring Boot application, React static assets, and generated Olist connector files. It runs in memory mode and does not require MySQL, Redis, or RabbitMQ.

From the repository root on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops-docker.ps1
```

The launcher builds the image, starts the container, waits for health checks, and seeds a complete task/report/approval/audit chain. Use `-Port 8081` when port 8080 is occupied.

If Docker Hub is unavailable in the current network:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-shopops-docker.ps1 -UseChinaMirror
```

Stop the demo:

```powershell
docker compose -p shopops-demo -f deploy/docker-compose.demo.yml down
```

Re-running the launcher after `down` starts with clean in-memory task data. The Docker image does not contain local raw datasets, secrets, evaluation files, or Git metadata.

## Development infrastructure

Development infrastructure:

```bash
docker compose -f deploy/docker-compose.dev.yml up -d
```

Services:

```text
MySQL    localhost:3306 / root:root / database: shopops_agent
Redis    localhost:6379
RabbitMQ localhost:5672
RabbitMQ Management UI http://localhost:15672 / shopops:shopops
```

The MySQL container only creates the `shopops_agent` database. Schema and seed data are applied by Flyway when `shopops-admin` starts with the `dev` profile.

Run the backend against these local services:

```bash
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Run the backend with RabbitMQ async task dispatch:

```bash
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--shopops.agent.dispatch-mode=rabbitmq"
```

In the default `sync` dispatch mode, task creation returns after execution finishes. In `rabbitmq` mode, task creation returns `QUEUED`, then the RabbitMQ consumer executes the task and updates it to `SUCCESS`, `DEGRADED`, or `FAILED`.

Optional RabbitMQ integration verification:

```bash
mvn -pl shopops-admin test "-Dshopops.rabbitmq.it=true" "-Dtest=AgentTaskRabbitDispatchIntegrationTest"
```

If you need to reset local infrastructure data from scratch:

```bash
docker compose -f deploy/docker-compose.dev.yml down -v
docker compose -f deploy/docker-compose.dev.yml up -d
```
