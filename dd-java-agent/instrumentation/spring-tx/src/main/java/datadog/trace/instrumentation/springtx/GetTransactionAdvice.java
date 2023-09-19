package datadog.trace.instrumentation.springtx;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetTransactionAdvice {
  private static final Logger log = LoggerFactory.getLogger(GetTransactionAdvice.class);
  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope onEnter(
//      @Advice.Argument(0) final TransactionDefinition definition
  ) {
    log.debug("------GetTransactionAdvice!!!-----");
//    AgentSpan agentSpan = startSpan("spring-tx");
//    DECORATE.afterStart(agentSpan);
//    DECORATE.spanNameForMethod(AbstractPlatformTransactionManager.class, "getTransaction");
//    if (definition != null) {
//      agentSpan.setTag(SPRING_ISOLATIONLEVEL, definition.getIsolationLevel());
//      agentSpan.setTag(
//          SPRING_PROPAGATIONBEHAVIOR, definition.getPropagationBehavior());
//      agentSpan.setTag(SPRING_TIMEOUT, definition.getTimeout());
//    }
//    return activateSpan(agentSpan);
    return null;
  }

//  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
//  public static void stopSpan(
//      @Advice.Enter final AgentScope scope,
//      @Advice.Thrown final Throwable throwable) {
//    if (scope == null) {
//      return;
//    }
//    DECORATE.onError(scope, throwable);
//    DECORATE.beforeFinish(scope);
//    scope.close();
//    scope.span().finish();
//  }
}
