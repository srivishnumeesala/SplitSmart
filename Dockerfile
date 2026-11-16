# ============================================================
# SplitSmart — Docker Build (Railway compatible)
# ============================================================

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy all source files
COPY src ./src

# Compile using shell script approach for maximum compatibility
RUN mkdir -p out && \
    find src -type f -name "*.java" > /tmp/sources.txt && \
    cat /tmp/sources.txt && \
    javac -d out @/tmp/sources.txt && \
    echo "=== Compilation successful ===" && \
    ls -la out/splitsmart/

# --- Runtime image ---
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/out ./out
COPY web ./web

RUN mkdir -p data

# Railway sets PORT dynamically
ENV PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:${PORT}/ || exit 1

CMD ["sh", "-c", "java -cp out splitsmart.WebMain"]
