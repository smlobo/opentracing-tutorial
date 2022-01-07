# Notes on this Fork

This fork of `opentracing-tutorial` does:
* Modifies the `lesson04` Solution for `docker-compose/k8s` env by:
  * getting Formatter/Publisher hosts/ports
  * getting Jaeger/apm-collector hosts/ports
  * automatically generates load from `Hello` to `Formatter/Publisher`
* Builds docker images from these
* Uses `docker-compose` to start with `Jaeger/apm-collector`
  ```
  % mvn clean package dependency:copy-dependencies
  % docker-compose -f docker-compose/docker-compose-jaeger.yml build --no-cache
  % docker-compose -f docker-compose/docker-compose-jaeger.yml up -d
  % docker-compose -f docker-compose/docker-compose-jaeger.yml down
  % docker-compose -f docker-compose/docker-compose-apm-collector.yml up -d
  % docker-compose -f docker-compose/docker-compose-apm-collector.yml down
  ```
* Uses `K8s` to start with `Jaeger/apm-collector`
  ```
  % mvn clean package dependency:copy-dependencies 
  % eval $(minikube docker-env)
  % docker-compose -f docker-compose/docker-compose-jaeger.yml build --no-cache
  % kubectl apply -f kubernetes/jaeger.yml
  % kubectl apply -f kubernetes/application/
  % minikube service jaeger-ui
  % kubectl delete -f kubernetes/application/
  % kubectl delete -f kubernetes/jaeger.yml
  % kubectl apply -f kubernetes/apm-collector.yml
  % kubectl apply -f kubernetes/application/
  % kubectl delete -f kubernetes/application
  % kubectl delete -f kubernetes/apm-collector.yml
  ```

# OpenTracing Tutorial - Java

## Installing

The tutorials are using CNCF Jaeger (https://github.com/jaegertracing/jaeger) as the tracing backend, 
[see here](../README.md) how to install it in a Docker image.

This repository uses Maven to manage dependencies. To install all dependencies, run:

```
cd opentracing-tutorial/java
mvn package
```

All subsequent commands in the tutorials should be executed relative to this `java` directory.

## Lessons

* [Lesson 01 - Hello World](./src/main/java/lesson01)
  * Instantiate a Tracer
  * Create a simple trace
  * Annotate the trace
* [Lesson 02 - Context and Tracing Functions](./src/main/java/lesson02)
  * Trace individual functions
  * Combine multiple spans into a single trace
  * Propagate the in-process context
* [Lesson 03 - Tracing RPC Requests](./src/main/java/lesson03)
  * Trace a transaction across more than one microservice
  * Pass the context between processes using `Inject` and `Extract`
  * Apply OpenTracing-recommended tags
* [Lesson 04 - Baggage](./src/main/java/lesson04)
  * Understand distributed context propagation
  * Use baggage to pass data through the call graph
* [Extra Credit](./src/main/java/extracredit)
  * Use existing open source instrumentation
