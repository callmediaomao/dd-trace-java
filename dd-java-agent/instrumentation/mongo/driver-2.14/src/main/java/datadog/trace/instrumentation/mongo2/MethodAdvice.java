package datadog.trace.instrumentation.mongo2;

import com.mongodb.AggregationOutput;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;

public class MethodAdvice {

  @Advice.OnMethodEnter
  public static AgentScope beforeMethod(
      @Advice.This final DBCollection dbCollection,
      @Advice.Origin final Method method
  ){
    /*ContextStore<DBCollection, String> contextStore = InstrumentationContext.get(DBCollection.class, String.class);
    String remotePeer = contextStore.get(dbCollection);*/
    AgentSpan agentSpan = startSpan(MongoDecorator.OPERATION_NAME);
    MongoDecorator.DECORATOR.afterStart(agentSpan);
    agentSpan.setTag("remotePeer",MongoDecorator.REMOTE_PEERS.get(dbCollection));
    MongoDecorator.DECORATOR.spanNameForMethod(method);
    return activateSpan(agentSpan);
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void stopSpan(
      @Advice.Enter final AgentScope scope,
      @Advice.Thrown final Throwable throwable,
      @Advice.Return final Object ret) {
    if (scope == null) {
      return;
    }
    CommandResult cresult = null;
    if (ret instanceof WriteResult) {
      WriteResult wresult = (WriteResult) ret;
      cresult = wresult.getCachedLastError();
    } else if (ret instanceof AggregationOutput) {
      AggregationOutput aresult = (AggregationOutput) ret;
      cresult = aresult.getCommandResult();
    }
    if (null != cresult && !cresult.ok()) {
      scope.span().setErrorMessage(cresult.getException().getMessage());
    }
    MongoDecorator.DECORATOR.onError(scope.span(), throwable);
    MongoDecorator.DECORATOR.beforeFinish(scope.span());
    scope.close();
    scope.span().finish();
  }
}
