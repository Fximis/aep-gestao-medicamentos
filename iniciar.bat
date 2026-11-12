@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
    echo Instale o JDK 21 ou superior e adicione a pasta bin do Java ao PATH.
    pause
    exit /b 1
)
if not exist bin mkdir bin
javac -encoding UTF-8 -cp "lib/*;src" -d bin src/auth/*.java src/models/*.java src/config/*.java src/dao/*.java src/views/*.java src/views/gui/*.java src/Main.java
if errorlevel 1 (
    echo Nao foi possivel compilar o projeto. Confira o erro acima.
    pause
    exit /b 1
)
java -cp "bin;lib/*" Main
if errorlevel 1 pause
