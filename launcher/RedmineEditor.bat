@echo off
set DIR="%~dp0\bin"
set JAVA_EXEC="%DIR:"=%\javaw"



pushd %DIR% & start "com.hiberus.anaya.redmineeditor" %JAVA_EXEC% %CDS_JVM_OPTS%  -p "%~dp0/../app" -m com.hiberus.anaya.redmineeditor/com.hiberus.anaya.redmineeditor.Main  %* & popd
