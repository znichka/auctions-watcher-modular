FROM eclipse-temurin:17-jre

COPY target/classes classes
COPY target/dependency dependency

ENTRYPOINT ["java","-cp","classes:dependency/*", "watcherbot.Starter"]
