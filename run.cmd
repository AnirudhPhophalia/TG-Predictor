@echo off
rem run.cmd demo  -> demo database with sample accounts and password sign-in
rem run.cmd       -> real database, Google sign-in (set the variables from .env.example first)
cd /d "%~dp0"
if /I "%~1"=="demo" (
    call mvnw.cmd javafx:run -Dapp.args=--demo
) else (
    call mvnw.cmd javafx:run
)
