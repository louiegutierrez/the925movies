# Build Stage: Uses Maven to compile and package the application
FROM maven:3.8.5-openjdk-11-slim AS builder
ARG MVN_PROFILE="default"
WORKDIR /app
COPY . .
RUN mvn clean package -P${MVN_PROFILE}

# Runtime Stage: Runs the WAR file inside Tomcat
FROM tomcat:10-jdk11
# Remove unnecessary WORKDIR line
COPY --from=builder /app/target/The925-1.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]