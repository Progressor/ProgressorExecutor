# Progressor - Executor

This is the code of the **Executor** component of the project **Progressor - The Programming Professor**.

## Deployment & Installation

These instructions are written for *Ubuntu* 16.04 LTS.

The goal is to install the Executor in `/opt/Executor/` and run it as a Docker container.

### Server setup

1. Install [*Docker*](https://www.docker.com/) by executing `curl -sSL https://get.docker.com/ | sh`
1. Install [*OpenJDK*](http://openjdk.java.net/) and [*Supervisor*](http://supervisord.org/) by executing `sudo apt-get install -y openjdk-8-jre-headless supervisor`
    * You may use any other *Java Runtime Environment* instead.
1. Configure *Supervisor* to start the **Executor** automatically:
    1. Set up *Supervisor* web admin interface with the username/password of your choice: `sudo nano /etc/supervisor/supervisord.conf`

        ```ini
        [inet_http_server]
        port = 9001
        username = your-supervisor-username
        password = your-supervisor-password
        ```

    1. With `sudo nano /etc/supervisor/conf.d/progressor-executor.conf`, create a supervisor service configuration, with the following content:

        ```ini
        [program:executor]
        command=java -jar /opt/Executor/ProgressorExecutor.jar
        autostart=true
        autorestart=true
        environment=KOTLIN_HOME="/kotlinc"
        ```

    1. Make *Supervisor* start when the server boots: `sudo systemctl enable supervisor`
    1. Start the service by executing `sudo service supervisor start`
    1. Reload *Supervisor* configuration by running `sudo supervisorctl reread`
    1. and `sudo supervisorctl update`

### Uploading the Executor jar and building the docker container

1. If you built the Executor jar from this repo, rename it to `ProgressorExecutor.jar` and upload it to your server in `/opt/Executor/`, otherwise download the pre-compiled one:
    1. `cd /opt/Executor/`
    1. `wget https://github.com/Progressor/ProgressorMeteor/raw/master/bin/ProgressorExecutor.jar`
1. If you modified the [*Dockerfile*](src/main/docker/Dockerfile), upload it to your server in `/opt/Executor/`, otherwise download the pre-compiled one:
    1. `cd /opt/Executor`
    1. `wget https://raw.githubusercontent.com/Progressor/ProgressorExecutor/master/src/main/docker/Dockerfile`
1. On the server, still in `/opt/Executor`, build the *Docker* container used by the Executor by running `docker build -t progressor/executor .`


# Building the Executor from source
## Maven

This repository contains a [*Maven*](https://maven.apache.org/) project created using [*IntelliJ IDEA*](https://www.jetbrains.com/idea/).

Executor won't work properly if Apache Thrift files have not been generated. Luckily, Maven generates these files for us when you build the project, so before you run the Executor run it as a Maven build: *mvn -clean -package*

### Dependencies

This project has four *Maven* dependencies:

1. [org.apache.thrift:libthrift:0.9.3](http://mvnrepository.com/artifact/org.apache.thrift/libthrift/0.9.3)
   for *Apache Thrift*
1. [org.json:json:20151123](http://mvnrepository.com/artifact/org.json/json/20151123)
   for *JSON* decoding
1. [org.slf4j:slf4j-simple:1.7.13](http://mvnrepository.com/artifact/org.slf4j/slf4j-simple/1.7.13)
   for logging
1. [org.testng:testng:RELEASE](http://mvnrepository.com/artifact/org.testng/testng)
   for unit tests

# Docker

[*Docker*](https://www.docker.com/) is active by default on Linux distributions. On Windows it is deactivated by default, since you're not able to use the Executor with *Docker* on Windows.
The created *Docker* image Tag is named `progressor/executor`.
If you decide not to use *Docker*, use the following `-docker false`.

Be aware that not using *Docker* requires you to install all the compilers of the languages you want to support on your server.
If you use *Docker* you need to install the compilers in your *Docker* image. To do that, you need to adjust the Dockerfile and rebuild your image.

# Programming Languages

The **Executor** currently supports five programming languages.

To use the languages, the following compilers (and other tools) need to be installed and available in the **PATH**.

1. *Java*: `javac` and `java`
1. *C/C++*: `g++`
1. *C#*: `csc` (Windows) or `msc` (Linux)
1. *Kotlin*: `kotlinc` and `kotlin`
1. *Python*: `python` (Windows)

As already mentioned, if you are using *Docker* these compilers need to be installed inside the *Docker* image via the Dockerfile.
Java is the only exception, since it is needed inside the *Docker* image as well as on the server to run the executor.

### Java

Support for *Java SE* version 8 is required.

The *Java Developer Toolkit* (JDK) can be downloaded from the [official *Oracle* downloads page](http://www.oracle.com/technetwork/java/javase/downloads/).

### C/C++

This projects targets the [*GNU Compiler Collection* (GCC)](https://gcc.gnu.org/).
Support for *C++11* is required.

* For Linux, install *g++* using `apt-get install g++`
* For Windows, the following packages are available:
  * [*MinGW*](http://www.mingw.org/), which can be downloaded from [*sourceforge*](https://sourceforge.net/projects/mingw/files/).
    * A [x64 version](http://mingw-w64.org/) is available on a [dedicated site](http://mingw-w64.org/doku.php/download/win-builds).
  * [*Cygwin*](http://sourceware.org/cygwin/), which can be downloaded from their home page.

### C# #

* On Windows, the C# compiler is already included in most recent Windows operatiog systems. Make sure that the *PATH* environment variable is set to `C:\WINDOWS\Microsoft.NET\Framework\v[your version number]\csc.exe.`
    * If this directory does not exist, you can download the [*.NET Core*](https://www.microsoft.com/net/download)
* On Linux, [*Mono*](http://www.mono-project.com/) can be downloaded on their [download page](http://www.mono-project.com/download/).

#### Kotlin

For [*Kotlin*](http://kotlinlang.org/), a [stand-alone compiler](http://kotlinlang.org/docs/tutorials/command-line.html) can be downloaded from [*GitHub*](https://github.com/JetBrains/kotlin/releases/latest).

### Python

* On Linux, Python 3 install using *apt-get install python3*
* On Windows, download [*Python 3*](https://www.python.org/downloads/release/python-351/) and install it.

## Extensibility

The **Executor** uses a [ServiceLoader](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the different code executors at runtime.

New languages can be supported by simply implementing the service *ch.bfh.progressor.executor.api.CodeExecutor* and making it discoverable by the **Executor**.
Additional information can be found in the [Java SE API Specification](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
