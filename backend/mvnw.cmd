@echo off
setlocal

cd /d "%~dp0"
set "MAVEN_PROJECTBASEDIR=%CD%"
java -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" "-Dmaven.user.home=%MAVEN_PROJECTBASEDIR%\.mvn" org.apache.maven.wrapper.MavenWrapperMain "-Dmaven.repo.local=%MAVEN_PROJECTBASEDIR%\.mvn\repository" %*
exit /b %ERRORLEVEL%
