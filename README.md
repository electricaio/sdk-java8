[![Build Status](https://travis-ci.com/electricaio/sdk-java8.svg?token=z1JRjGpmpeeKYqo3ypqF&branch=master)](https://travis-ci.com/electricaio/sdk-java8)
[![Coverage Status](https://coveralls.io/repos/github/electricaio/sdk-java8/badge.svg?branch=master&t=cRCvXK)](https://coveralls.io/github/electricaio/sdk-java8?branch=master)

# About
Electrica.io SDK libraries project for Java.

# Structure
## Core
Common API classes to provide connection with Electrica.io infrastructure.

## Core HTTP
Default implementation of `HttpModule` using OkHttp client.

## Dependencies
Module that provide dependency management (BOM).

# Artifacts repository
We use our own public maven repository to provide artifacts `http://maven-repo.electrica.io`.

## Maven
Specify one more repository in your `pom.xml`

```
<repositories>
    <repository>
        <id>Electrica</id>
        <url>http://maven-repo.electrica.io</url>
    </repository>
</repositories>
```

## Gradle
Specify repository in your `build.gradle`
```
repositories {
    maven {
        url 'http://maven-repo.electrica.io'
    }
}
```


# Dependency management
To not care about versions compatibility we provide BOM dependency `io.electrica.sdk.java8:dependencies`, that allows
customers use dependency management approach in their projects.

## Maven
Maven has two possible approaches to use Electrica.io Java SDK dependency management.
The first and recommended is to import BOM dependency in `dependencyManagement` section:

```
<dependencies>
    <dependency>
        <groupId>io.electrica.sdk.java8</groupId>
        <artifactId>echo</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.electrica.sdk.java8</groupId>
            <artifactId>dependencies</artifactId>
            <version>0.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Also it is possible to specify BOM dependency as a parent project, but you will constrained to use only one BOM
dependency and it doesn't work for subprojects.
```
<parent>
    <groupId>io.electrica.sdk.java8</groupId>
    <artifactId>dependencies</artifactId>
    <version>0.0.1</version>
</parent>

<dependencies>
    <dependency>
        <groupId>io.electrica.sdk.java8</groupId>
        <artifactId>echo</artifactId>
    </dependency>
</dependencies>
```

Maven provides a simple way to override the version of any dependency in BOM using properties:
```
<properties>
  <electrica-sdk-java8-echo.version>0.0.2</electrica-sdk-java8-echo.version>
</properties>
```
See full list of properties in `dependencies` module pom.

## Gradle
We recommend to use Spring `io.spring.dependency-management` Gradle plugin for dependency management.
```
apply plugin: 'io.spring.dependency-management'

dependencyManagement {
    imports {
        mavenBom 'io.electrica.sdk.java8:dependencies:0.0.1'
    }
}

dependencies {
    compile 'io.electrica.sdk.java8:echo'
}
```


# Complete example

## Maven
```
<repositories>
    <repository>
        <id>Electrica</id>
        <url>http://maven-repo.electrica.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.electrica.sdk.java8</groupId>
        <artifactId>core</artifactId>
    </dependency>
    <dependency>
        <groupId>io.electrica.sdk.java8</groupId>
        <artifactId>echo</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.electrica.sdk.java8</groupId>
            <artifactId>dependencies</artifactId>
            <version>0.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Gradle
```
plugins {
    id 'java'
    id 'io.spring.dependency-management' version '1.0.6.RELEASE'
}

apply plugin: 'io.spring.dependency-management'

repositories {
    mavenCentral()
    maven {
        url 'http://maven-repo.electrica.io'
    }
}

dependencyManagement {
    imports {
        mavenBom 'io.electrica.sdk.java8:dependencies:0.0.1'
    }
}

dependencies {
    compile 'io.electrica.sdk.java8:core'
    compile 'io.electrica.sdk.java8:echo'
}
```