#!/bin/bash
set -e
echo "Compiling SplitSmart..."
javac -d out -sourcepath src src/splitsmart/Main.java
echo "Launching SplitSmart..."
java -cp out splitsmart.Main
