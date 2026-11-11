#!/bin/bash
set -e
echo "Compiling SplitSmart Web Application..."
javac -d out -sourcepath src src/splitsmart/WebMain.java
echo "Launching SplitSmart on http://localhost:8080 ..."
java -cp out splitsmart.WebMain
