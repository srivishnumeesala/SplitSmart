@echo off
setlocal
echo Compiling tests...
javac -d out -sourcepath src src\splitsmart\test\SettlementTest.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Running tests...
java -cp out splitsmart.test.SettlementTest
endlocal
