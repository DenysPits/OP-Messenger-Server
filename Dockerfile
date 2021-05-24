FROM maven:3.6.1-jdk-8-alpine AS MAVEN_BUILD

RUN mkdir /app
WORKDIR /app
COPY . /app

# package our application code
RUN mvn clean compile assembly:single

# the second stage of our build will use open jdk 8 on alpine 3.9
FROM openjdk:8-jre-alpine3.9

# copy only the artifacts we need from the first stage and discard the rest
COPY --from=MAVEN_BUILD /app/target/ServerMessenger-1.0-jar-with-dependencies.jar /messenger.jar
COPY --from=MAVEN_BUILD /app /app

# set the startup command to execute the jar
EXPOSE 8000
CMD ["java", "-jar", "/messenger.jar"]