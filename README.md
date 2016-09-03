# Progressor - Executor

This is the code **Executor** component of the project **Progressor - The Programming Professor**.

## Deployment & Installation

These instructions are written for *Ubuntu* 16.04.1 LTS.

Note: you may have to prepend some or all of the commands with `sudo` depending on your environment.

1. Install [*Docker*](https://www.docker.com/) by executing `curl -sSL https://get.docker.com/ | sh` on the server.
2. Install [*OpenJDK*](http://openjdk.java.net/) and [*Supervisor*](http://supervisord.org/) by executing `apt-get install -y openjdk-8-jre-headless supervisor` on the server.
   * Of course, you may use any other *Java Runtime Environment* instead.
3. Copy the neccessary files to the server.
   1. The compiled *JAR*-version of the **Executor** by executing `scp -P 2201 <path-to-jar> <server-user>@<server-host>:<path-to-server-directory>` on the development machine.
   2. The *Dockerfile* by executing `scp -P 2201 <path-to-dockerfile> <server-user>@<server-host>:<path-to-server-directory>` on the development machine.
4. Build the *Docker* container used by the Executor by executing `docker build -t progressor/executor .` on the server.
5. Configure *Supervisor* to start the **Executor** automatically.
   1.  Update the *Supervisor* configuration file by executing `echo "[inet_http_server]" | tee -a /etc/supervisor/supervisord.conf`,
   2.  `echo "port = 9001`,
   3.  `echo "username = <supervisor-username>`,
   4.  and `echo "password = <supervisor-password>" | tee -a /etc/supervisor/supervisord.conf` on the server.
   5.  Create the service configuration file by executing `echo "[program:executor]" | tee /etc/supervisor/conf.d/progressor-executor.conf`,
   6.  `echo "command=java -jar /opt/Executor/ProgressorExecutor-1.0-jar-with-dependencies.jar" | tee -a /etc/supervisor/conf.d/progressor-executor.conf`,
   7.  `echo "autostart=true" | tee -a /etc/supervisor/conf.d/progressor-executor.conf`,
   8.  `echo "autostart=true" | tee -a /etc/supervisor/conf.d/progressor-executor.conf`,
   9.  and `echo "environment=KOTLIN_HOME=\"/kotlinc\"" | tee -a /etc/supervisor/conf.d/progressor-executor.conf` on the server.
   10. Force *Supervisor* to apply the new configuration `sudo supervisorctl reread`
   11. and `sudo supervisorctl update` on the server.
   12. Start the service by executing `service supervisor start` on the server.

## Maven

This repository contains a [*Maven*](https://maven.apache.org/) project created using [*IntelliJ IDEA*](https://www.jetbrains.com/idea/).

Executor won't work properly if apachethrift files have not been gernerated. Luckely Maven generates these files for us when you build the project.
So before you run the Executor run it as a Maven build: *mvn -clean -package*

### Dependencies

This project has four *Maven* dependencies:

1. [org.apache.thrift:libthrift:0.9.3](http://mvnrepository.com/artifact/org.apache.thrift/libthrift/0.9.3)
   for *Apache Thrift*
2. [org.json:json:20151123](http://mvnrepository.com/artifact/org.json/json/20151123)
   for *JSON* decoding
3. [org.slf4j:slf4j-simple:1.7.13](http://mvnrepository.com/artifact/org.slf4j/slf4j-simple/1.7.13)
   for logging
3. [org.testng:testng:RELEASE](http://mvnrepository.com/artifact/org.testng/testng)
   for unit tests

## Docker

[*Docker*](https://www.docker.com/) is active by default on Linux distributions. On Windows it is deactivated by default, since you're not able to use the Executor with *Docker* on Windows.
The created *Docker* image Tag is named `progressor/executor`.
If you decide to not user *Docker*, you can specified with starting arguments `-docker false`.

Be aware, that not using *Docker* requires you to install all the compilers of the languages you want to support on your server.
If you use *Docker* you need to install the compilers in your *Docker* image. To do that, you need to adjust the Dockerfile and rebuild your image.

## Programming Languages

The **Executor** currently supports four programming languages.

To use the languages, the following compilers (and other tools) need to be installed and available in the **PATH**.

1. *Java*: `javac` and `java`
2. *C/C++*: `g++`
3. *C#*: `csc` (Windows) or `msc` (Linux)
4. *Kotlin*: `kotlinc` and `kotlin`
5. *Python*: `python` (Windows)

As already mentioned, if you are using *Docker* these compilers need to be installed inside the *Docker* image via the Dockerfile.
Java is the only exception, since it is needen inside the *Docker* image as well as on the server to run the executor.

### Java

Support for *Java SE* version 8 is required.

The *Java Developer Toolkit* (JDK) can be downloaded from the [official *Oracle* downloads page](http://www.oracle.com/technetwork/java/javase/downloads/).

### C/C++

This projects targets the [*GNU Compiler Collection* (GCC)](https://gcc.gnu.org/).
Support for *C++11* is required.

* For Linux, install *G++* using `apt-get install g++`
* For Windows, the following packages are available:
  * [*MinGW*](http://www.mingw.org/), which can be downloaded from [*sourceforge*](https://sourceforge.net/projects/mingw/files/).
    * A [x64 version](http://mingw-w64.org/) is available on a [dedicated site](http://mingw-w64.org/doku.php/download/win-builds).
  * [*Cygwin*](http://sourceware.org/cygwin/), which can be downloaded from their home page.

### C#

* For Windows, C# compiler is already part of the recent Windows operationg systems.
  Make sure that the *PATH* environment variable is set to `C:\WINDOWS\Microsoft.NET\Framework\v[your version number]\csc.exe.`
    * If directory does not exist you can download the [*.NET Core*](https://www.microsoft.com/net/download)
* For Linux, [*Mono*](http://www.mono-project.com/) can be downloaded from their [downloads page](http://www.mono-project.com/download/).

#### Kotlin

For [*Kotlin*](http://kotlinlang.org/), a [stand-alone compiler](http://kotlinlang.org/docs/tutorials/command-line.html) can be downloaded from [*GitHub*](https://github.com/JetBrains/kotlin/releases/latest).

### Python

* For Linux, Python 3 install using *apt-get install python3*
* For Windows, Download [*Python 3*](https://www.python.org/downloads/release/python-351/) and install.

## Extensibility

The **Executor** uses a [ServiceLoader](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the different code executors at runtime.

New languages can be supported simply by implementing the service *ch.bfh.progressor.executor.api.CodeExecutor* and making it discoverable by the **Executor**.
Additional information can be found in the [Java SE API Specification](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
