############################################################
# Dockerfile to build sandbox for executing user code			#
# Based on Ubuntu																					#
############################################################

FROM chug/ubuntu14.04x64
MAINTAINER Thomas Strub, Marc Touw, Janick Weidmann

# Update the repository sources list
RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
RUN apt-get upgrade

#########################################################
#Install all the languages/compilers we are supporting.##
#########################################################

#Installing C\C++(GCC & G++) compiler, python3 and mono
#Curl and unzip are later used for the Kotlin installation
RUN apt-get update && \
 apt-get install -y g++ \
 gcc \
 python3-pip \
 mono-mcs \
 curl \
 unzip


#Install Java 8
RUN \
    echo "===> add webupd8 repository..."  && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list  && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list  && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886  && \
    apt-get update  && \
    \
    \
    echo "===> install Java"  && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections  && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections  && \
    DEBIAN_FRONTEND=noninteractive  apt-get install -y --force-yes oracle-java8-installer oracle-java8-set-default  && \
    \
    \
    echo "===> clean up..."  && \
    rm -rf /var/cache/oracle-jdk8-installer  && \
    apt-get clean  && \
    rm -rf /var/lib/apt/lists/*

#Install Kotlin
RUN curl -O -J -L https://github.com/JetBrains/kotlin/releases/download/1.0.2/kotlin-compiler-1.0.2.zip
RUN mkdir -p /kotlin-compiler
RUN unzip kotlin-compiler-1.0.2.zip -d /kotlin-compiler

#Setting Kotlin environment variables
ENV KOTLIN_HOME /kotlin-compiler/kotlinc
ENV PATH $KOTLIN_HOME/bin:$PATH

#Define working dir
WORKDIR /opt
# define default command
CMD ["bash"]
