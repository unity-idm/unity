# syntax=docker/dockerfile:1.5

##########
# Build stage
##########
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src

# 1) Copy only all pom.xml files (auto-discovered), preserving paths
# This keeps dependency resolution cacheable even for large multi-module repos.
RUN --mount=type=bind,src=.,dst=/context,readonly \
    mkdir -p /src \
 && cd /context \
 && find . -name pom.xml -print0 \
 | tar --null -T - -cf - \
 | tar -xf - -C /src

# 2) Pre-fetch dependencies (cache ~/.m2 across builds)
RUN --mount=type=cache,dst=/root/.m2 \
    mvn -B -T 1C -DskipTests -e dependency:go-offline

# 3) Copy full sources (generic)
RUN --mount=type=bind,src=.,dst=/context,readonly \
    tar -cf - -C /context . \
 | tar -xf - -C /src

# 4) Build in parallel
RUN --mount=type=cache,dst=/root/.m2 \
   mvn -B -T 1C -DskipTests package \
 && mkdir /src/built-target \
 && tar -xzf distribution/target/*.tar.gz -C /src/built-target

##########
# Runtime stage
##########
FROM eclipse-temurin:21-jre-jammy
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"
WORKDIR /app

# Adjust this path to the module that produces the runnable artifact
COPY --from=build /src/built-target/* /app/

EXPOSE 2443
ENTRYPOINT ["/app/bin/unity-idm-server-start", "-f"]

