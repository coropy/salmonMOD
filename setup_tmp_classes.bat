@echo off
set LOG=C:\dev\salmonMOD\setup_log.txt
echo === setup_tmp_classes start === > %LOG%

set JARPATH=%USERPROFILE%\.gradle\caches\fabric-loom\26.2\minecraft-server.jar
set DEST=C:\dev\salmonMOD\tmp_classes

echo JARPATH=%JARPATH% >> %LOG%
echo DEST=%DEST% >> %LOG%

if not exist "%JARPATH%" (
    echo ERROR: jar not found >> %LOG%
    goto :end
)

if not exist "%DEST%" (
    mkdir "%DEST%" 2>> %LOG%
    echo Created %DEST% >> %LOG%
)

cd /d "%DEST%" 2>> %LOG%
echo Current dir: %CD% >> %LOG%

jar xf "%JARPATH%" net/minecraft/server/level/ServerPlayer.class 2>> %LOG%
echo jar exit code: %ERRORLEVEL% >> %LOG%

echo === Files extracted === >> %LOG%
dir /s /b "%DEST%" >> %LOG% 2>&1

:end
echo === setup_tmp_classes end === >> %LOG%