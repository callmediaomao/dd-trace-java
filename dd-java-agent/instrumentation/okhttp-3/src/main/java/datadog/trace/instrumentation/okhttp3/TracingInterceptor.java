package datadog.trace.instrumentation.okhttp3;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.decorator.HttpClientDecorator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.*;
import static datadog.trace.instrumentation.okhttp3.OkHttpClientDecorator.DECORATE;
import static datadog.trace.instrumentation.okhttp3.OkHttpClientDecorator.OKHTTP_REQUEST;
import static datadog.trace.instrumentation.okhttp3.RequestBuilderInjectAdapter.SETTER;

public class TracingInterceptor implements Interceptor {
  @Override
  public Response intercept(final Chain chain) throws IOException {
    if (chain.request().header("Datadog-Meta-Lang") != null) {
      // 已经有链路，直接执行下一个过滤器
      return chain.proceed(chain.request());
    }

    final AgentSpan span = startSpan(OKHTTP_REQUEST);

    try (final AgentScope scope = activateSpan(span)) {
      DECORATE.afterStart(span);
      DECORATE.onRequest(span, chain.request());

      final Request.Builder requestBuilder = chain.request().newBuilder();
      propagate().inject(span, requestBuilder, SETTER);
      propagate()
          .injectPathwayContext(
              span, requestBuilder, SETTER, HttpClientDecorator.CLIENT_PATHWAY_EDGE_TAGS);

      final Response response;
      try {
        // 执行下一个
        response = chain.proceed(requestBuilder.build());
      } catch (final Exception e) {
        DECORATE.onError(span, e);
        throw e;
      }

      DECORATE.onResponse(span, response);
      DECORATE.beforeFinish(span);
      return response;
    } finally {
      span.finish();
    }
  }
}
