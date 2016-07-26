#######################################################
# Dockerfile to build sandbox for executing user code #
# Based on Ubuntu 14.04 x64                           #
#######################################################

FROM chug/ubuntu14.04x64
MAINTAINER Thomas Strub, Marc Touw, Janick Weidmann

#Update the repository sources list
RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade -y --force-yes

##########################################################
# Install all the languages/compilers we are supporting ##
##########################################################

#Install system tools curl and unzip
RUN apt-get install -y curl unzip

#Configure Mono 4
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF && \
    echo "deb http://download.mono-project.com/repo/debian wheezy main" | tee /etc/apt/sources.list.d/mono-xamarin.list && \
    echo "deb http://download.mono-project.com/repo/debian wheezy-apache24-compat main" | tee -a /etc/apt/sources.list.d/mono-xamarin.list

#Configure Node.js 6
RUN curl -sL https://deb.nodesource.com/setup_6.x | bash -

#Configure PHP 7.0
RUN apt-get install -y software-properties-common && \
    add-apt-repository ppa:ondrej/php

#Install C\C++ compiler (GCC & G++), Python 3, Mono, Node.js, and PHP
RUN apt-get update && \
    apt-get install -y --force-yes g++ gcc \
    python3-pip \
    mono-mcs mono-vbnc \
    nodejs \
    php7.0

#Install Java 8
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886 && \
    apt-get update && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --force-yes oracle-java8-installer oracle-java8-set-default && \
    rm -rf /var/cache/oracle-jdk8-installer && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

#Install Kotlin
RUN curl -O -J -L https://github.com/JetBrains/kotlin/releases/download/1.0.2/kotlin-compiler-1.0.2.zip && \
    unzip kotlin-compiler-1.0.2.zip -d / && \
    rm kotlin-compiler-1.0.2.zip

#Set Kotlin environment variables
ENV KOTLIN_HOME /kotlinc
ENV PATH $KOTLIN_HOME/bin:$PATH

#Update any available packages
RUN apt-get update && \
    apt-get dist-upgrade -y --force-yes && \
    apt-get autoremove

#########################
# Docker configuration ##
#########################

#Define working directory
WORKDIR /opt

#Define default command
CMD ["bash"]
