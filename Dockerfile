FROM gradle as builder

WORKDIR /home/gradle
RUN git clone https://github.com/UnitTestBot/jacodb
WORKDIR /home/gradle/jacodb
RUN gradle publishToMavenLocal

COPY --chown=gradle:gradle . /home/gradle/project
WORKDIR /home/gradle/project
RUN gradle shadowJar --no-daemon
RUN ls -al build/libs/


# FROM openjdk:11-jre-slim
FROM openjdk:11

WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/*.jar analyzer.jar
ENTRYPOINT ["java", "-jar", "analyzer.jar"]
