# ============================================================
# SplitSmart — Docker Build (Railway compatible)
# Using full JDK in both stages so Railway can find javac
# ============================================================

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY src ./src
RUN mkdir -p out \
    && find src -type f -name "*.java" > /tmp/sources.txt \
    && javac -d out @/tmp/sources.txt \
    && echo "Build complete — compiled classes:"  \
    && ls out/splitsmart/

# Runtime: use full JDK (not JRE) so Railway can find javac
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/out ./out
COPY web ./web

RUN mkdir -p data

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "out", "splitsmart.WebMain"]
