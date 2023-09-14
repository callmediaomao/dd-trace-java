package datadog.trace.instrumentation.springtx;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.*;
import static datadog.trace.instrumentation.springtx.SpringTxDecorator.DECORATE;

public class GetTransactionAdvice {

  private static final Logger log = LoggerFactory.getLogger(GetTransactionAdvice.class);

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope onEnter(
      @Advice.Argument(0) final TransactionDefinition definition
  ) {
    log.debug("--------getTracsaction------");
    AgentSpan agentSpan = startSpan("spring-tx");
    DECORATE.afterStart(agentSpan);
    DECORATE.spanNameForMethod(AbstractPlatformTransactionManager.class, "getTransaction");
    if (definition != null) {
      agentSpan.setTag(SPRING_ISOLATIONLEVEL, definition.getIsolationLevel());
      agentSpan.setTag(
          SPRING_PROPAGATIONBEHAVIOR, definition.getPropagationBehavior());
      agentSpan.setTag(SPRING_TIMEOUT, definition.getTimeout());
    }
    return activateSpan(agentSpan);
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void stopSpan(
      @Advice.Enter final AgentScope scope,
      @Advice.Thrown final Throwable throwable) {
    if (scope == null) {
      return;
    }
    DECORATE.onError(scope, throwable);
    DECORATE.beforeFinish(scope);
    scope.close();
    scope.span().finish();
  }
}
