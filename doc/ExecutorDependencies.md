# Progressor - Executor Dependencies

This project has the following *Maven* dependencies.

1. [org.apache.thrift:libthrift:0.10.0](http://mvnrepository.com/artifact/org.apache.thrift/libthrift/0.10.0)
   for *Apache Thrift*
1. [org.json:json:20170516](http://mvnrepository.com/artifact/org.json/json/20170516)
   for *JSON* decoding
1. [org.slf4j:slf4j-simple:1.7.25](http://mvnrepository.com/artifact/org.slf4j/slf4j-simple/1.7.25)
   for logging
1. [org.testng:testng:6.11](http://mvnrepository.com/artifact/org.testng/testng/6.11)
   for unit tests
1. [commons-io:commons-io:2.5](http://mvnrepository.com/artifact/commons-io/commons-io/2.5)
   for proper *UTF-8* support on streams
1. [javax.annotation:javax.annotation-api:1.3.1](https://mvnrepository.com/artifact/javax.annotation/javax.annotation-api/1.3.1)
   for generated thrift code

And relies on the following plugins.

1. [org.apache.maven.plugins:maven-compiler-plugin:3.7.0](http://mvnrepository.com/artifact/org.apache.maven.plugins/maven-compiler-plugin/3.7.0)
   for targeting JDK 1.8
1. [org.apache.maven.plugins:maven-assembly-plugin:3.1.0](http://mvnrepository.com/artifact/org.apache.maven.plugins/maven-assembly-plugin/3.1.0)
   for specifying the main class and additional assembly settings
1. [org.apache.thrift.tools:maven-thrift-plugin:0.1.11](http://mvnrepository.com/artifact/org.apache.thrift.tools/maven-thrift-plugin/0.1.11)
   for automatically generating Java and Node.js thrift libraries
1. [org.codehaus.mojo:build-helper-maven-plugin:1.12](http://mvnrepository.com/artifact/org.codehaus.mojo/build-helper-maven-plugin/1.12)
   for including the previously generated Java library

## Docker

[*Docker*](https://www.docker.com/) support is activated by default on Linux distributions. On any other platform, *Docker* is not yet supported.
The created *Docker* image is tagged `progressor/executor`.
If you decide not to use *Docker*, use the following `-docker false`.

Be aware that not using *Docker* requires you to install all the compilers of the languages you want to support on your server.
If you use *Docker* you need to install the compilers in your *Docker* image. To do that, you need to adjust the *Dockerfile* and rebuild your image.

## Programming Languages

The **Executor** currently supports five programming languages.

To use the languages, the following compilers (and other tools) need to be installed and available in the **PATH**.

1. *Java*: `javac` and `java`
1. *C++*: `g++`
1. *C#*: `csc` (Windows) or `msc` and `mono` (Linux)
1. *Python*: `python` (Windows)
1. *JavaScript* ([Node.js](https://nodejs.org/)): `node` 
1. *PHP*: `php`
1. *Kotlin*: `kotlinc` and `kotlin`
1. *VB.NET*: `vbc` (Windows) or `vbnc` and `mono` (Linux)

As already mentioned, if you are using *Docker* these compilers need to be installed inside the *Docker* image via the *Dockerfile*.
Java is the only exception, since it is needed inside the *Docker* image as well as on the server to run the **Executor**.

### Java

Support for *Java 8* is required.

The *Java Developer Toolkit* (JDK) can be downloaded from the [official *Oracle* downloads page](http://www.oracle.com/technetwork/java/javase/downloads/).

### C++

This projects targets the [*GNU Compiler Collection* (GCC)](https://gcc.gnu.org/).
Support for *C++11* is required.

* For Linux, install *g++* using `apt-get install g++`
* For Windows, the following packages are available:
  * [*MinGW*](http://www.mingw.org/), which can be downloaded from [*sourceforge*](https://sourceforge.net/projects/mingw/files/).
    * A [x64 version](http://mingw-w64.org/) is available on a [dedicated site](http://mingw-w64.org/doku.php/download/win-builds).
  * [*Cygwin*](http://sourceware.org/cygwin/), which can be downloaded from their home page.

### C# #

* On Windows, the C# compiler is already included in most recent Windows operating systems. Make sure that the *PATH* environment variable is set to `C:\WINDOWS\Microsoft.NET\Framework\v[your version number]\csc.exe.`
    * If this directory does not exist, you can download the [*.NET Core*](https://www.microsoft.com/net/download).
* On Linux, [*Mono*](http://www.mono-project.com/) can be downloaded on their [download page](http://www.mono-project.com/download/).

### Python

* On Linux, Python 3 install using *apt-get install python3*
* On Windows, download [*Python 3*](https://www.python.org/downloads/release/python-351/) and install it.

### JavaScript

Install [Node.js](https://nodejs.org/) version *6.x.x* as any version below will not fully comply with *ES6*.

### PHP

Install [PHP](http://php.net/downloads.php) ([Windows](http://windows.php.net/download#php-7.0)) version 7.x.x.

### Kotlin

For [*Kotlin*](http://kotlinlang.org/), a [stand-alone compiler](http://kotlinlang.org/docs/tutorials/command-line.html) can be downloaded from [*GitHub*](https://github.com/JetBrains/kotlin/releases/latest).

### VB.NET

* On Windows, the VB.NET compiler is already included in most recent Windows operating systems. Make sure that the *PATH* environment variable is set to `C:\WINDOWS\Microsoft.NET\Framework\v[your version number]\vbc.exe.`
    * If this directory does not exist, you can download the [*.NET Core*](https://www.microsoft.com/net/download).
* On Linux, [*Mono*](http://www.mono-project.com/) can be downloaded on their [download page](http://www.mono-project.com/download/).
