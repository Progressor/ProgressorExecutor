# Progressor - Executor

This is the code **Executor** component of the project **Progressor - The Programming Professor**.

## Maven

This repository contains a [*Maven*](https://maven.apache.org/) project created using [*IntelliJ IDEA*](https://www.jetbrains.com/idea/).

...

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

...

## Programming Languages

The **Executor** currently supports four programming languages.

To use the languages, the following compilers (and other tools) need to be installed and available in the **PATH**.

1. *Java*: `javac` and `java`
2. *C/C++*: `g++`
3. *C#*: `csc` (Windows) or `msc` (Linux)
4. *Kotlin*: `kotlinc` and `kotlin`
5. *Python*: `python`

### Java

Support for *Java SE* version 8 is required.

The *Java Developer Toolkit* (JDK) can be downloaded from the [official *Oracle* downloads page](http://www.oracle.com/technetwork/java/javase/downloads/).

### C/C++

This projects targets the [*GNU Compiler Collection* (GCC)](https://gcc.gnu.org/).
Support for *C++11* is required.

* For Linux, ...
* For Windows, the following packages are available:
  * [*MinGW*](http://www.mingw.org/), which can be downloaded from [*sourceforge*](https://sourceforge.net/projects/mingw/files/).
    * A [x64 version](http://mingw-w64.org/) is available on a [dedicated site](http://mingw-w64.org/doku.php/download/win-builds).
  * [*Cygwin*](http://sourceware.org/cygwin/), which can be downloaded from their home page.

### C#

* For Windows, ...
* For Linux, [*Mono*](http://www.mono-project.com/) can be downloaded from their [downloads page](http://www.mono-project.com/download/).

#### Kotlin

For [*Kotlin*](http://kotlinlang.org/), a [stand-alone compiler](http://kotlinlang.org/docs/tutorials/command-line.html) can be downloaded from [*GitHub*](https://github.com/JetBrains/kotlin/releases/latest).

### Python

...

### Extensibility

The **Executor** uses a [ServiceLoader](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the different code executors at runtime.

New languages can be supported simply by implementing the service *ch.bfh.progressor.executor.api.CodeExecutor* and making it discoverable by the **Executor**.
Additional information can be found in the [Java SE API Specification](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
