@echo off
setlocal
echo Compiling SplitSmart...
javac -d out -sourcepath src src\splitsmart\Main.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Launching SplitSmart...
java -cp out splitsmart.Main
endlocal
