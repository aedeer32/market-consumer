FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/market-consumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]