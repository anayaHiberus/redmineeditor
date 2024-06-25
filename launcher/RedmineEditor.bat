@echo off
set DIR="%~dp0\bin"

IF /I "%COMSPEC%" == %CMDCMDLINE% GOTO runHere
IF NOT [%1]==[] GOTO runHere

REM running from double click, run in background
set JAVA_EXEC=start "com.hiberus.anaya.redmineeditor" "%DIR:"=%\javaw"
goto launch

:runHere
REM running from command line, run here
set JAVA_EXEC="%DIR:"=%\java"

: launch
pushd %DIR% & %JAVA_EXEC% %CDS_JVM_OPTS% -Djava.net.useSystemProxies=true -p "%~dp0/../app" -m com.hiberus.anaya.redmineeditor/com.hiberus.anaya.redmineeditor.Main %* & popd
