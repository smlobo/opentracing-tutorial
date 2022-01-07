# Notes on this Fork

See the `java` README

# OpenTracing Tutorials

A collection of tutorials for the OpenTracing API (https://opentracing.io).

## Tutorials by Language

  * [C# tutorial](./csharp/)
  * [Go tutorial](./go/)
  * [Java tutorial](./java)
  * [Python tutorial](./python)
  * [Node.js tutorial](./nodejs)

Also check out examples from the book [Mastering Distributed Tracing](https://www.shkuro.com/books/2019-mastering-distributed-tracing/):
* [Chapter 4: Instrumentation Basics with OpenTracing](https://github.com/PacktPublishing/Mastering-Distributed-Tracing/tree/master/Chapter04)
* [Chapter 5: Instrumentation of Asynchronous Applications](https://github.com/PacktPublishing/Mastering-Distributed-Tracing/tree/master/Chapter05)
* [Chapter 7: Tracing with Service Mesh](https://github.com/PacktPublishing/Mastering-Distributed-Tracing/tree/master/Chapter07)
* [Chapter 11: Integration with Metrics and Logs](https://github.com/PacktPublishing/Mastering-Distributed-Tracing/tree/master/Chapter11)
* [Chapter 12: Gathering Insights Through Data Mining](https://github.com/PacktPublishing/Mastering-Distributed-Tracing/tree/master/Chapter12)


## Prerequisites

The tutorials are using CNCF Jaeger (https://jaegertracing.io) as the tracing backend.
For this tutorial, we'll start Jaeger via Docker with the default in-memory storage, exposing only the required ports. We'll also enable "debug" level logging:

```
docker run \
  --rm \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 16686:16686 \
  jaegertracing/all-in-one:1.7 \
  --log-level=debug
```

Alternatively, Jaeger can be downloaded as a binary called `all-in-one` for different platforms from https://jaegertracing.io/download/.

Once the backend starts, the Jaeger UI will be accessible at http://localhost:16686.
