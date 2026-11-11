# Multi-stage lightweight JDK build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY src ./src
RUN mkdir -p out && javac -d out -sourcepath src src/splitsmart/WebMain.java

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/out ./out
COPY web ./web

# Persistent data directory
RUN mkdir -p data

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "out", "splitsmart.WebMain"]
