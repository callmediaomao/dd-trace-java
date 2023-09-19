package datadog.trace.instrumentation.mongo2;

import com.mongodb.DBCollection;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.mongo2.MongoDecorator.DECORATOR;

public class ReVoidMethodAdvice {
  @Advice.OnMethodEnter
  public static AgentScope beforeMethod(
      @Advice.This final DBCollection dbCollection,
      @Advice.Origin final Method method
  ){
    ContextStore<DBCollection, String> contextStore = InstrumentationContext.get(DBCollection.class, String.class);
    String remotePeer = contextStore.get(dbCollection);
    AgentSpan agentSpan = startSpan(MongoDecorator.OPERATION_NAME);
    DECORATOR.afterStart(agentSpan);
    agentSpan.setTag("remotePeer",remotePeer);
    DECORATOR.spanNameForMethod(method);
    return activateSpan(agentSpan);
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void stopSpan(
      @Advice.Enter final AgentScope scope,
      @Advice.Thrown final Throwable throwable) {
    if (scope == null) {
      return;
    }
    DECORATOR.onError(scope.span(), throwable);
    DECORATOR.beforeFinish(scope.span());
    scope.close();
    scope.span().finish();
  }
}
