# ============================================================
# SplitSmart — Docker Build
# Java 21 JDK, no external dependencies
# ============================================================

# --- Stage 1: Compile ---
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy source
COPY src ./src

# Compile: use a file list to avoid xargs issues with many files
RUN mkdir -p out \
    && find src -name "*.java" > sources.txt \
    && javac -d out @sources.txt

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy compiled classes and web frontend
COPY --from=builder /app/out ./out
COPY web ./web

# Data folder for CSV persistence
RUN mkdir -p data

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "out", "splitsmart.WebMain"]
