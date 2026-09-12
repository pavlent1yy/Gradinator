FROM maven:3.9-eclipse-temurin-17 AS build

ARG MODULE

ENV TZ=Europe/Moscow
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Moscow"

WORKDIR /build

# Сначала POM'ы — это позволяет Docker кэшировать зависимости
COPY pom.xml .
COPY g-api/pom.xml g-api/pom.xml
COPY g-core/pom.xml g-core/pom.xml

RUN mvn -pl ${MODULE} -am dependency:go-offline

# Исходники
COPY g-api/src g-api/src
COPY g-core/src g-core/src

# Сборка нужного модуля
RUN mvn -pl ${MODULE} -am clean package -DskipTests


# =========================
# Runtime
# =========================

FROM eclipse-temurin:17-jre

ARG MODULE

WORKDIR /app

COPY --from=build /build/${MODULE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]