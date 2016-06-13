# Progressor - Executor

This is the code **Executor** component of the project **Progressor - The Programming Professor**.

### Extensibility

The **Executor** uses a [ServiceLoader](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the different code executors at runtime.

New languages can be supported simply by implementing the service *ch.bfh.progressor.executor.api.CodeExecutor* and making it discoverable by the **Executor**.
Additional information can be found in the [Java SE API Specification](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

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

Docker is active by default on Linux distributions. On Windows it is deactivated by default, since you're not able to use the Executor with docker on Windows.
The created dockerimage Tag is named: *progressor/executor*
If you decide to not user docker, you can specified with starting arguments:*-docker false*

Be aware, that not using docker requires you to install all the compilers of the languages you want to support on your server. If you use docker 
you need to install the compilers in your docker image. To do that, you need to adjust the Dockerfile and rebuild your image.

## Programming Languages

The **Executor** currently supports four programming languages.

To use the languages, the following compilers (and other tools) need to be installed and available in the **PATH**.

1. *Java*: `javac` and `java`
2. *C/C++*: `g++`
3. *C#*: `csc` (Windows) or `msc` (Linux)
4. *Kotlin*: `kotlinc` and `kotlin`
5. *Python*: `python` (Windows)

As already mentioned, if you are using docker these compilers need to be installed inside the Docker image via the Dockerfile.
Java is the only exception, since it is needen inside the Docker image as well as on the server to run the executor.

### Java

Support for *Java SE* version 8 is required.

The *Java Developer Toolkit* (JDK) can be downloaded from the [official *Oracle* downloads page](http://www.oracle.com/technetwork/java/javase/downloads/).

### C/C++

This projects targets the [*GNU Compiler Collection* (GCC)](https://gcc.gnu.org/).
Support for *C++11* is required.

* For Linux, G++, Install using *apt-get install g++*
* For Windows, the following packages are available:
  * [*MinGW*](http://www.mingw.org/), which can be downloaded from [*sourceforge*](https://sourceforge.net/projects/mingw/files/).
    * A [x64 version](http://mingw-w64.org/) is available on a [dedicated site](http://mingw-w64.org/doku.php/download/win-builds).
  * [*Cygwin*](http://sourceware.org/cygwin/), which can be downloaded from their home page.

### C#

* For Windows, C# compiler is already part of the recent Windows operationg systems. Make sure that the **PATH** environment variable is set to *C:\WINDOWS\Microsoft.NET\Framework\v[your version number]\csc.exe.*
    * If directory does not exist you can download the [*.Net Core*](https://www.microsoft.com/net/download)
* For Linux, [*Mono*](http://www.mono-project.com/) can be downloaded from their [downloads page](http://www.mono-project.com/download/).

#### Kotlin

For [*Kotlin*](http://kotlinlang.org/), a [stand-alone compiler](http://kotlinlang.org/docs/tutorials/command-line.html) can be downloaded from [*GitHub*](https://github.com/JetBrains/kotlin/releases/latest).

### Python

* For Linux, Python 3 install using *apt-get install python3*
* For Windows, Download [*Python 3*](https://www.python.org/downloads/release/python-351/) and install.


