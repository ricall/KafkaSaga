FROM openjdk:11-jre-slim
EXPOSE 8080

WORKDIR /app
COPY build/libs/KafkaSaga-0.0.1-SNAPSHOT.jar /app/application.jar
CMD java -jar application.jar