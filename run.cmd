@echo off
cd /d "%~dp0"
if /I "%~1"=="demo" set SPRING_PROFILES_ACTIVE=demo
if /I "%~1"=="postgres" set SPRING_PROFILES_ACTIVE=postgres
call mvnw.cmd spring-boot:run
