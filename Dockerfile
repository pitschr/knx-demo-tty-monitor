#
# Build stage
#
FROM maven:3.6.3-jdk-11-slim AS build
COPY src /tmp/build/src
COPY pom.xml /tmp/build
RUN mvn -f /tmp/build/pom.xml clean package

#
# Package stage
#
FROM adoptopenjdk/openjdk11:jre-11.0.5_10-alpine
# add for 'tput' command to fetch the terminal size
RUN apk add ncurses
# add fake symbol as alpine has no /bin/bash
RUN ln -s /bin/sh /bin/bash
# copy the build package
COPY --from=build /tmp/build/target/knx-demo-tty-monitor.jar /knx-demo-tty-monitor.jar
