@echo off
setlocal
echo Compiling SplitSmart Web Application...
javac -d out -sourcepath src src\splitsmart\WebMain.java
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Launching SplitSmart on http://localhost:8080 ...
java -cp out splitsmart.WebMain
endlocal
