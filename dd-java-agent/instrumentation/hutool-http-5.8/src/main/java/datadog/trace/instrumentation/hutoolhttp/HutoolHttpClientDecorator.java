package datadog.trace.instrumentation.hutoolhttp;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.HttpClientDecorator;

import java.net.URI;
import java.net.URISyntaxException;

public class HutoolHttpClientDecorator extends HttpClientDecorator<HttpRequest, HttpResponse> {
  public static final CharSequence OKHTTP = UTF8BytesString.create("okhttp");
  public static final HutoolHttpClientDecorator DECORATE = new HutoolHttpClientDecorator();
  public static final CharSequence OKHTTP_REQUEST =
      UTF8BytesString.create(DECORATE.operationName());

  @Override
  protected String method(HttpRequest request) {
    return request.getMethod().name();
  }

  @Override
  protected URI url(HttpRequest request) throws URISyntaxException {
    String url = request.getUrl();
    return new URI(url);
  }

  @Override
  protected int status(HttpResponse response) {
    return response.getStatus();
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"okhttp", "okhttp-2"};
  }

  @Override
  protected CharSequence component() {
    return OKHTTP;
  }

  @Override
  protected String getRequestHeader(HttpRequest request, String headerName) {
    return request.header(headerName);
  }

  @Override
  protected String getResponseHeader(HttpResponse response, String headerName) {
    return response.header(headerName);
  }
}
