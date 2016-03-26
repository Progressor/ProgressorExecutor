# Progressor - Executor

This is the code **Executor** component of the project **Progressor - The Programming Professor**.

## Instructions

This repository contains a *Maven* project created using *IntelliJ IDEA*.

### Maven

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

#### Languages

The **Executor** currently supports four programming languages.

To use the languages, the following compilers (and other tools) need to be installed and available in the **PATH**.

1. Java: `javac` and `java`
2. C/C++: `g++`
3. C#: `csc` (Windows) or `msc` (Linux)
4. Kotlin: `kotlinc` and `kotlin`

#### Java

Java 8 ...

#### C/C++

GCC, C++11 ...

#### C#

...

#### Kotlin

...

#### Extensibility

The **Executor** uses a [ServiceLoader](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the different code executors at runtime.

New languages can be supported simply by implementing the service *ch.bfh.progressor.executor.CodeExecutor* and making it discoverable by the **Executor**.
Additional information can be found in the [Java SE API Specification](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
