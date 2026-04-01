@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

set JAVA_EXE="C:\Program Files\Android\Android Studio\jbr\bin\java.exe"

%JAVA_EXE% -jar "%CLASSPATH%" %*

endlocal