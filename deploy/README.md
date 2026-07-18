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

The MySQL container initializes the P0 schema and seed data from `sql/`.

If you need to re-run SQL initialization from scratch:

```bash
docker compose -f deploy/docker-compose.dev.yml down -v
docker compose -f deploy/docker-compose.dev.yml up -d
```

