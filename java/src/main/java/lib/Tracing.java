package lib;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Tracing {
    private static final Logger logger = LoggerFactory.getLogger(Tracing.class);

    private Tracing() {
    }

    public static JaegerTracer init(String service) {
        SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        Configuration.SenderConfiguration senderConfig = Configuration.SenderConfiguration.fromEnv();
        // K8s
        String k8sJaegerHost = System.getenv("JAEGER_COMPACT_SERVICE_HOST");
        String apmCollectorHost = System.getenv("APM_COLLECTOR_SERVICE_HOST");
        if (k8sJaegerHost != null)
            senderConfig.withAgentHost(k8sJaegerHost);
        else if (apmCollectorHost != null)
            senderConfig.withAgentHost(apmCollectorHost);
        else if (Files.exists(Paths.get("/.dockerenv")))
            senderConfig.withAgentHost("tracer");
        logger.info("Using Tracer Host: " + senderConfig.getAgentHost());

        ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv()
                .withLogSpans(true)
                .withSender(senderConfig);

        Configuration config = new Configuration(service)
                .withSampler(samplerConfig)
                .withReporter(reporterConfig);

        return config.getTracer();
    }

    public static Span startServerSpan(Tracer tracer, javax.ws.rs.core.HttpHeaders httpHeaders, String operationName) {
        // format the headers for extraction
        MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
        final HashMap<String, String> headers = new HashMap<String, String>();
        for (String key : rawHeaders.keySet()) {
            headers.put(key, rawHeaders.get(key).get(0));
        }

        Tracer.SpanBuilder spanBuilder;
        try {
            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(headers));
            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan(operationName);
            } else {
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan(operationName);
        }
        // TODO could add more tags like http.url
        return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).start();
    }

    public static TextMap requestBuilderCarrier(final Request.Builder builder) {
        return new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("carrier is write-only");
            }

            @Override
            public void put(String key, String value) {
                builder.addHeader(key, value);
            }
        };
    }
}
