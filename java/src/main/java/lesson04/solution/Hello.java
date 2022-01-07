package lesson04.solution;

import com.google.common.collect.ImmutableMap;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import lib.Tracing;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Hello {

    private final Tracer tracer;
    private final OkHttpClient client;
    private static final Logger logger = LoggerFactory.getLogger(Hello.class);
    private static String formatterHost;
    private static String formatterPort;
    private static String publisherHost;
    private static String publisherPort;

    private Hello(Tracer tracer) {
        this.tracer = tracer;
        this.client = new OkHttpClient();
    }

    private String getHttp(String host, int port, String path, String param, String value) {
        try {
            HttpUrl url = new HttpUrl.Builder().scheme("http").host(host).port(port).addPathSegment(path)
                    .addQueryParameter(param, value).build();
            Request.Builder requestBuilder = new Request.Builder().url(url);
            
            Span activeSpan = tracer.activeSpan();
            Tags.SPAN_KIND.set(activeSpan, Tags.SPAN_KIND_CLIENT);
            Tags.HTTP_METHOD.set(activeSpan, "GET");
            Tags.HTTP_URL.set(activeSpan, url.toString());
            tracer.inject(activeSpan.context(), Format.Builtin.HTTP_HEADERS, Tracing.requestBuilderCarrier(requestBuilder));

            Request request = requestBuilder.build();
            Response response = client.newCall(request).execute();

            Tags.HTTP_STATUS.set(activeSpan, response.code());
            if (response.code() != 200) {
                throw new RuntimeException("Bad HTTP result: " + response);
            }
            return response.body().string();
        } catch (Exception e) {
            Tags.ERROR.set(tracer.activeSpan(), true);
            tracer.activeSpan().log(ImmutableMap.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, e));
            throw new RuntimeException(e);
        }
    }

    private void sayHello(String helloTo, String greeting) {
        Span span = tracer.buildSpan("say-hello").start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            span.setTag("hello-to", helloTo);
            span.setBaggageItem("greeting", greeting);

            String helloStr = formatString(helloTo);
            printHello(helloStr);
        } finally {
            span.finish();
        }
    }

    private String formatString(String helloTo) {
        Span span = tracer.buildSpan("formatString").start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            String helloStr = getHttp(formatterHost, Integer.parseInt(formatterPort), "format", "helloTo", helloTo);
            span.log(ImmutableMap.of("event", "string-format", "value", helloStr));
            return helloStr;
        } finally {
            span.finish();
        }
    }

    private void printHello(String helloStr) {
        Span span = tracer.buildSpan("printHello").start();
        try (Scope scope = tracer.scopeManager().activate(span)) {
            getHttp(publisherHost, Integer.parseInt(publisherPort), "publish", "helloStr", helloStr);
            span.log(ImmutableMap.of("event", "println"));
        } finally {
            span.finish();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Delay start for docker environment
        boolean isDocker = Files.exists(Paths.get("/.dockerenv"));
        if (isDocker)
            Thread.sleep(5000);
        // Sheldon - formatter service
        formatterHost = System.getenv("FORMATTER_SERVICE_HOST");
        if (formatterHost == null && isDocker)
            formatterHost = "formatter";
        if (formatterHost == null)
            formatterHost = "localhost";
        logger.info("Using Formatter Host: " + formatterHost);
        formatterPort = System.getenv("FORMATTER_SERVICE_PORT");
        if (formatterPort == null)
            formatterPort = "8081";
        logger.info("Using Formatter Port: " + formatterPort);
        // Sheldon - publisher service
        publisherHost = System.getenv("PUBLISHER_SERVICE_HOST");
        if (publisherHost == null && isDocker)
            publisherHost = "publisher";
        if (publisherHost == null)
            publisherHost = "localhost";
        logger.info("Using Publisher Host: " + publisherHost);
        publisherPort = System.getenv("PUBLISHER_SERVICE_PORT");
        if (publisherPort == null)
            publisherPort = "8082";
        logger.info("Using Publisher Port: " + publisherPort);

        if (args.length == 2) {
            String helloTo = args[0];
            String greeting = args[1];

            try (JaegerTracer tracer = Tracing.init("hello-world")) {
                new Hello(tracer).sayHello(helloTo, greeting);
            }
        }
        else {
            try (JaegerTracer tracer = Tracing.init("hello-world")) {
                for (int i = 0; i < 100000; i++) {
                    new Hello(tracer).sayHello("test", "g" + i);
                    Thread.sleep(5000);
                }
            }
        }
    }
}
