ARG BUILDER_IMAGE=eclipse-temurin:17-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre

FROM ${BUILDER_IMAGE} AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY pom.xml .

COPY shopops-common/pom.xml shopops-common/pom.xml
COPY shopops-admin/pom.xml shopops-admin/pom.xml
COPY shopops-commerce-mcp-server/pom.xml shopops-commerce-mcp-server/pom.xml

COPY shopops-common/src shopops-common/src
COPY shopops-admin/src shopops-admin/src
COPY shopops-commerce-mcp-server/src shopops-commerce-mcp-server/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl shopops-admin -am package -DskipTests

FROM ${RUNTIME_IMAGE}

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /workspace/shopops-admin/target/shopops-admin-*.jar /app/shopops-admin.jar
COPY docs/demo-data/olist /app/docs/demo-data/olist

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=6 \
    CMD curl --fail --silent http://localhost:8080/admin/workbench.html > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/shopops-admin.jar"]