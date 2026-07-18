# Deploy

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

If you need to reset local infrastructure data from scratch:

```bash
docker compose -f deploy/docker-compose.dev.yml down -v
docker compose -f deploy/docker-compose.dev.yml up -d
```
