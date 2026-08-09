# Stage 8B-R Environment

Java was pinned to `C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot` for Maven runs.

- Bare `java -version`: Java 8 appeared first on PATH before pinning.
- Pinned Java runtime: OpenJDK 17.0.19 Microsoft.
- `javac`: 17.0.19.
- Maven: Apache Maven 3.9.16, running on Java 17.0.19.
- Docker: Docker Desktop 4.82.0, engine 29.6.1.
- Docker Compose: v5.3.0.
- MySQL: Docker container `shopops-demo-shopops-mysql-1`, MySQL 8.4.10.
- Spring profile: default.
- JDBC URL host: `localhost:3306`.

Secrets are redacted from reports.
