@echo off
setlocal enabledelayedexpansion
rem KCube Link Explorer build script (Maven Tycho) - Windows version of build.sh.
rem
rem Usage:
rem   build.bat             build and leave the plug-in jar and update-site zip in dist\
rem   build.bat --install   build, then install into %ECLIPSE_HOME%\dropins (restart Eclipse with -clean)
rem
rem Environment:
rem   JAVA_HOME     JDK 17 (required unless the default java is already 17)
rem   ECLIPSE_HOME  Eclipse install directory containing dropins\ (required for --install)

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
cd /d "%ROOT%"

call mvn -q -B clean verify
if errorlevel 1 exit /b 1

if exist "%ROOT%\dist" rd /s /q "%ROOT%\dist"
mkdir "%ROOT%\dist"

set "JAR="
for %%F in ("%ROOT%\com.kcube.link\target\com.kcube.link-*.jar") do if not defined JAR set "JAR=%%~fF"
if not defined JAR (
	echo plug-in jar not found in com.kcube.link\target
	exit /b 1
)

rem File names use major.minor.micro only; Bundle-Version inside the jar keeps the qualifier.
set "VERSION="
for /f "usebackq delims=" %%V in (`powershell -NoProfile -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $z=[IO.Compression.ZipFile]::OpenRead('%JAR%'); try { $r=New-Object IO.StreamReader($z.GetEntry('META-INF/MANIFEST.MF').Open()); $m=$r.ReadToEnd() } finally { $z.Dispose() }; if ($m -match '(?m)^Bundle-Version:\s*(\d+\.\d+\.\d+)') { $Matches[1] }"`) do set "VERSION=%%V"
if not defined VERSION (
	echo failed to read Bundle-Version from %JAR%
	exit /b 1
)

copy /y "%JAR%" "%ROOT%\dist\com.kcube.link.kcube-link-explorer-%VERSION%.jar" >nul
set "ZIP="
for %%F in ("%ROOT%\com.kcube.link.update-site\target\com.kcube.link.update-site-*.zip") do if not defined ZIP set "ZIP=%%~fF"
copy /y "%ZIP%" "%ROOT%\dist\kcube-link-explorer-update-site-%VERSION%.zip" >nul
echo built: dist\com.kcube.link.kcube-link-explorer-%VERSION%.jar

if /i "%~1"=="--install" (
	if not defined ECLIPSE_HOME (
		echo Set ECLIPSE_HOME to the Eclipse install directory that contains dropins\
		exit /b 1
	)
	if not exist "!ECLIPSE_HOME!\dropins" (
		echo ECLIPSE_HOME\dropins not found: !ECLIPSE_HOME!
		exit /b 1
	)
	rem Remove old installs first, otherwise Eclipse may load a stale version.
	del /q "!ECLIPSE_HOME!\dropins\com.kcube.link_*.jar" 2>nul
	del /q "!ECLIPSE_HOME!\dropins\com.kcube.link.kcube-link-explorer-*.jar" 2>nul
	copy /y "%ROOT%\dist\com.kcube.link.kcube-link-explorer-%VERSION%.jar" "!ECLIPSE_HOME!\dropins\" >nul
	echo installed to !ECLIPSE_HOME!\dropins - restart Eclipse with -clean
)
endlocal
