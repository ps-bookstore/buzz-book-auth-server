
FROM eclipse-temurin:21

WORKDIR /app

COPY /target/buzz-book-auth-server-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]