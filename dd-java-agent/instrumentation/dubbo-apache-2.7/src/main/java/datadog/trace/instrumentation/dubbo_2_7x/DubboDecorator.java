package datadog.trace.instrumentation.dubbo_2_7x;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.BaseDecorator;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.*;
import static datadog.trace.instrumentation.dubbo_2_7x.DubboConstants.*;
import static datadog.trace.instrumentation.dubbo_2_7x.DubboHeadersExtractAdapter.GETTER;
import static datadog.trace.instrumentation.dubbo_2_7x.DubboHeadersInjectAdapter.SETTER;

public class DubboDecorator extends BaseDecorator {
  private static final Logger log = LoggerFactory.getLogger(DubboDecorator.class);
  public static final CharSequence DUBBO_REQUEST = UTF8BytesString.create("dubbo");

  public static final CharSequence DUBBO_SERVER = UTF8BytesString.create("apache-dubbo");

  public static final DubboDecorator DECORATE = new DubboDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[]{"apache-dubbo"};
  }

  @Override
  protected CharSequence spanType() {
    return DUBBO_SERVER;
  }

  @Override
  protected CharSequence component() {
    return DUBBO_SERVER;
  }

  public AgentSpan startDubboSpan(Invoker invoker,Invocation invocation) {
    URL url = invoker.getUrl();
    boolean isConsumer = isConsumerSide(url);
    log.debug("isConsumer:{},invoker name:{}",isConsumer,invoker.getClass().getName());
    log.debug("isConsumer:{},invocation:{}",isConsumer,invocation.getClass().getName());

    String methodName = invocation.getMethodName();
    String resourceName = generateOperationName(invoker,url,invocation);
    if (!isConsumer&& META_RESOURCE.equals(invoker.getInterface().getName())){
      log.debug("skip span because dubbo resourceName:{}",META_RESOURCE);
      return activeSpan();
    }
    String shortUrl = generateRequestURL(invoker,url,invocation);
    if (log.isDebugEnabled()) {
      log.debug("isConsumer:{},method:{},resourceName:{},shortUrl:{},longUrl:{},version:{}",
          isConsumer,
          methodName,
          resourceName,
          shortUrl,
          url.toString(),
          getVersion(url)
          );
    }
    AgentSpan span;

    DubboTraceInfo dubboTraceInfo = new DubboTraceInfo((RpcInvocation) invocation,RpcContext.getContext());

    if (isConsumer){
      // this is consumer
      span = startSpan(DUBBO_REQUEST);
    }else{
      // this is provider
      AgentSpan.Context parentContext = propagate().extract(dubboTraceInfo, GETTER);
      span = startSpan(DUBBO_REQUEST,parentContext);
    }
    span.setTag(TAG_URL, url.toString());
    span.setTag(TAG_SHORT_URL, shortUrl);
    span.setTag(TAG_METHOD, methodName);
    span.setTag(TAG_VERSION,getVersion(url));

    span.setTag(TAG_SIDE,isConsumer?CONSUMER_SIDE:PROVIDER_SIDE);

    afterStart(span);

    withMethod(span, resourceName);
    propagate().inject(span, dubboTraceInfo, SETTER);
    return span;
  }

  public void withMethod(final AgentSpan span, final String methodName) {
    span.setResourceName(methodName);
  }

  @Override
  public AgentSpan afterStart(AgentSpan span) {
    return super.afterStart(span);
  }


  private String providerResourceName(Invoker invoker,Invocation invocation){
    StringBuilder operationName = new StringBuilder();
  //  operationName.append(invoker.getInterface().getName());
    if(invoker.getInterface()!=null){
      operationName.append(invoker.getInterface().getName());
    }else{
      operationName.append(invoker.getClass().getName());
    }

    operationName.append("." + invocation.getMethodName() + "(");
    for (Class<?> classes : invocation.getParameterTypes()) {
      operationName.append(classes.getSimpleName() + ",");
    }
    if (invocation.getParameterTypes().length > 0) {
      operationName.delete(operationName.length() - 1, operationName.length());
    }
    operationName.append(")");
    return operationName.toString();
  }

  private String generateOperationName(Invoker invoker,URL requestURL, Invocation invocation) {
    boolean isConsumer = isConsumerSide(requestURL);
    if (isConsumer) {
      StringBuilder operationName = new StringBuilder();
      String groupStr = requestURL.getParameter(GROUP_KEY);
      groupStr = StringUtils.isEmpty(groupStr) ? "" : groupStr + "/";
      operationName.append(groupStr);
      operationName.append(requestURL.getPath());
      operationName.append("." + invocation.getMethodName() + "(");
      for (Class<?> classes : invocation.getParameterTypes()) {
        operationName.append(classes.getSimpleName() + ",");
      }
      if (invocation.getParameterTypes().length > 0) {
        operationName.delete(operationName.length() - 1, operationName.length());
      }
      operationName.append(")");
      return operationName.toString();
    }else{
      return providerResourceName(invoker,invocation);
    }

  }

  private String generateRequestURL(Invoker invoker,URL url, Invocation invocation) {
    StringBuilder requestURL = new StringBuilder();
    requestURL.append(url.getProtocol() + "://");
    requestURL.append(url.getHost());
    requestURL.append(":" + url.getPort() + "/");
    requestURL.append(generateOperationName(invoker,url, invocation));
    return requestURL.toString();
  }

  public boolean isConsumerSide(URL url) {
    return url.getParameter(SIDE_KEY, PROVIDER_SIDE).equals(CONSUMER_SIDE);
  }

  public AgentScope buildSpan(Invoker invoker, Invocation invocation) {
    AgentSpan span = startDubboSpan(invoker,invocation);
    if (span==null){
      return activeScope();
    }
    AgentScope agentScope = activateSpan(span);
    return agentScope;
  }

  private String getVersion(URL url){
    return url.getParameter(VERSION);
  }

  private String getHostAddress(Invocation invocation) {
    final URL url = invocation.getInvoker().getUrl();
    return HostAndPort.toHostAndPortString(url.getHost(), url.getPort());
  }
}
