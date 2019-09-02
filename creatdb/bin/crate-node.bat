@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  crate-node startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and CRATE_NODE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\main;%APP_HOME%\lib\main;%APP_HOME%\lib\users.jar;%APP_HOME%\lib\functions.jar;%APP_HOME%\lib\license.jar;%APP_HOME%\lib\crate-sql.jar;%APP_HOME%\lib\crate-blob.jar;%APP_HOME%\lib\crate-http-transport.jar;%APP_HOME%\lib\crate-udc.jar;%APP_HOME%\lib\crate-azure-discovery.jar;%APP_HOME%\lib\crate-dns-discovery.jar;%APP_HOME%\lib\es-discovery-ec2.jar;%APP_HOME%\lib\es-repository-url.jar;%APP_HOME%\lib\es-repository-s3.jar;%APP_HOME%\lib\es-repository-azure.jar;%APP_HOME%\lib\ssl-impl.jar;%APP_HOME%\lib\es-transport.jar;%APP_HOME%\lib\es-analysis-common.jar;%APP_HOME%\lib\xbean-finder-4.5.jar;%APP_HOME%\lib\crate-dex.jar;%APP_HOME%\lib\crate-sql-parser.jar;%APP_HOME%\lib\crate-shared.jar;%APP_HOME%\lib\ssl.jar;%APP_HOME%\lib\crate-common.jar;%APP_HOME%\lib\t-digest-3.2.jar;%APP_HOME%\lib\HdrHistogram-2.1.9.jar;%APP_HOME%\lib\netty-codec-http-4.1.36.Final.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.36.Final.jar;%APP_HOME%\lib\netty-handler-4.1.36.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.1.36.Final.jar;%APP_HOME%\lib\netty-codec-4.1.36.Final.jar;%APP_HOME%\lib\netty-transport-4.1.36.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.36.Final.jar;%APP_HOME%\lib\azure-storage-8.0.0.jar;%APP_HOME%\lib\azure-keyvault-core-1.0.0.jar;%APP_HOME%\lib\guava-27.1-jre.jar;%APP_HOME%\lib\aws-java-sdk-s3-1.11.576.jar;%APP_HOME%\lib\commons-math3-3.6.1.jar;%APP_HOME%\lib\jackson-dataformat-csv-2.5.1.jar;%APP_HOME%\lib\aws-java-sdk-ec2-1.11.576.jar;%APP_HOME%\lib\aws-java-sdk-kms-1.11.576.jar;%APP_HOME%\lib\aws-java-sdk-core-1.11.576.jar;%APP_HOME%\lib\jmespath-java-1.11.576.jar;%APP_HOME%\lib\jackson-databind-2.8.11.jar;%APP_HOME%\lib\jaxb-api-2.2.2.jar;%APP_HOME%\lib\es-server.jar;%APP_HOME%\lib\jsr305-3.0.1.jar;%APP_HOME%\lib\es-x-content.jar;%APP_HOME%\lib\jackson-dataformat-cbor-2.8.11.jar;%APP_HOME%\lib\jackson-dataformat-smile-2.8.11.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.8.11.jar;%APP_HOME%\lib\jackson-core-2.8.11.jar;%APP_HOME%\lib\httpclient-4.5.2.jar;%APP_HOME%\lib\httpcore-4.4.5.jar;%APP_HOME%\lib\commons-logging-1.1.3.jar;%APP_HOME%\lib\commons-codec-1.10.jar;%APP_HOME%\lib\jackson-annotations-2.8.11.jar;%APP_HOME%\lib\netty-resolver-4.1.36.Final.jar;%APP_HOME%\lib\netty-common-4.1.36.Final.jar;%APP_HOME%\lib\xbean-bundleutils-4.5.jar;%APP_HOME%\lib\slf4j-api-1.6.2.jar;%APP_HOME%\lib\es-core.jar;%APP_HOME%\lib\log4j-1.2-api-2.11.1.jar;%APP_HOME%\lib\log4j-core-2.11.1.jar;%APP_HOME%\lib\log4j-api-2.11.1.jar;%APP_HOME%\lib\lucene-suggest-8.0.0.jar;%APP_HOME%\lib\lucene-analyzers-common-8.0.0.jar;%APP_HOME%\lib\lucene-backward-codecs-8.0.0.jar;%APP_HOME%\lib\lucene-grouping-8.0.0.jar;%APP_HOME%\lib\lucene-join-8.0.0.jar;%APP_HOME%\lib\lucene-misc-8.0.0.jar;%APP_HOME%\lib\lucene-queries-8.0.0.jar;%APP_HOME%\lib\lucene-sandbox-8.0.0.jar;%APP_HOME%\lib\lucene-spatial-8.0.0.jar;%APP_HOME%\lib\lucene-spatial-extras-8.0.0.jar;%APP_HOME%\lib\lucene-spatial3d-8.0.0.jar;%APP_HOME%\lib\lucene-core-8.0.0.jar;%APP_HOME%\lib\hppc-0.7.1.jar;%APP_HOME%\lib\antlr4-runtime-4.7.2.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\checker-qual-2.5.2.jar;%APP_HOME%\lib\error_prone_annotations-2.2.0.jar;%APP_HOME%\lib\j2objc-annotations-1.1.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.17.jar;%APP_HOME%\lib\stax-api-1.0-2.jar;%APP_HOME%\lib\activation-1.1.jar;%APP_HOME%\lib\elasticsearch-cli-7.0.0.jar;%APP_HOME%\lib\joda-time-2.10.1.jar;%APP_HOME%\lib\spatial4j-0.7.jar;%APP_HOME%\lib\jts-core-1.15.0.jar;%APP_HOME%\lib\jna-4.2.2.jar;%APP_HOME%\lib\ion-java-1.0.2.jar;%APP_HOME%\lib\commons-lang3-3.5.jar;%APP_HOME%\lib\s2-geometry-library-java-1.0.0.jar;%APP_HOME%\lib\jopt-simple-5.0.2.jar;%APP_HOME%\lib\snakeyaml-1.17.jar;%APP_HOME%\lib\crate-app.jar

@rem Execute crate-node
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %CRATE_NODE_OPTS%  -classpath "%CLASSPATH%" org.elasticsearch.cluster.coordination.NodeToolCli %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable CRATE_NODE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%CRATE_NODE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
