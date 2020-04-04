## Introduction
This repository contains example code for a simple JWT relying party built with [Eclipse Vert.x](http://vertx.io). It
demonstrates the following
* How to configure the Vert.x HTTP server to support TLS
* How to validate a JWT (issued by a companion example, vertx-jwt-idp)
* How to set a routing context's user principal

## Requirements
To build this example, you will need the following:
1. A version of Git capable of cloning this repository from Git Hub
1. Apache Maven v3.5 or greater
1. The latest patch release of OpenJDK 11 (build produced by the [AdoptOpenJDK Project](https://adoptopenjdk.net/) work
nicely)

## Building the Project
You may build the example in one of two ways, as a JAR or a Docker image. 
### Maven Build
You may build JAR from source using [Apache Maven](http://maven.apache.org). Assuming a version >= 3.5.0 you can build it  by
executing `mvn package` at the command line (assuming `mvn` is in the path, of course). In the project's /target
directory, this will produce
* A JAR file named __vertx-jwt-rp-1.0.jar__, which contains just the project's classes
* A fat JAR named __vertx-jwt-rp-1.0-fat.jar__; you can use this to run the code by executing `java -jar VertXJpa-1.0-fat.jar`
at your favorite command line.
### Building as a Docker Image
You may use the included Dockerfile to create a deployable image. From the source directory, run the following
command to build the image: `docker build -t vertxjwtrp:1.0 .`. Here, the resulting image will have the tag
__vertxjwtrp:1.0__. 

Run the container with the following command: `docker run --rm -p 9443:9443 --name vertxjwtrp vertxjwtrp:1.0`. You will 
be able to connect to the app at https://localhost:9443.

## Configuring the Example
The example includes a default configuration that creates an API verticle bound to port TCP/8443

The TCP port and other behaviors may be customized by setting the following properties either as OS environment
variables or JRE system proprties (i.e. "-D" properties). The latter have a higher priority than the former.
| Property          | Notes                                                        |
| ----------------- | ------------------------------------------------------------ |
| bind-port         | An integer value that sets the API verticle's TCP bind port. |
| rp-keystore       | The absolute path to a Java key store (.jks) containing a TLS private key and certificate.  This parameter is required! |
| rp-keystore-password | The password for idp-key-store. | 

## Running the Example
Unless configured otherwise, the application presents a single RESTful endpoint on port TCP/9443 that consumes a JWT and
 echoes its contents back as a JSON object. Send a GET request to https://localhost:9443/api/hello, include the JWT as a
 bearer token in the Authorization header.
    
If all goes well, you will receive an application/json response that looks like this:
```json
{
    "iss": "vertxjwt",
    "sub": "6fe630e9-7e07-4ceb-9887-41e195a07917",
    "iat": 1586039,
    "nbf": 1586039,
    "jti": "83651c94-5880-4cf5-9dc4-cbc6a0a64f46",
    "exp": 1586043241,
    "rol": [
        "basic"
    ]
}
```
I recommend [Postman](https://www.postman.com/) to exercise the example, although any tool capable of generating the
necessary HTTP requests will suffice.

