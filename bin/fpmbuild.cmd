@echo off
setlocal
set JAR=%~dp0..\target\fpmbuild-java-0.0.1-SNAPSHOT.jar
if not "%FPMBUILD_JAR%"=="" set JAR=%FPMBUILD_JAR%
java -jar "%JAR%" %*
