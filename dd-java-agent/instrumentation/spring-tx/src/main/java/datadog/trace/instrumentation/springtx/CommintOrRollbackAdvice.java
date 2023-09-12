package datadog.trace.instrumentation.springtx;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.*;
import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.SPRING_ISCOMPLETED;
import static datadog.trace.instrumentation.springtx.SpringTxDecorator.DECORATE;

public class CommintOrRollbackAdvice {
  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope onEnter(
      @Advice.Argument(0) final TransactionStatus transactionStatus
  ) {
    AgentSpan agentSpan = startSpan("spring-tx");
    DECORATE.afterStart(agentSpan);
    DECORATE.spanNameForMethod(AbstractPlatformTransactionManager.class, "commit/rollback");
    agentSpan.setTag(SPRING_ISNEWTRANSACTION, transactionStatus.isNewTransaction());
    agentSpan.setTag(
        SPRING_HASAVEPOINT, transactionStatus.hasSavepoint());
    agentSpan.setTag(SPRING_ISROLLBACKONLY, transactionStatus.isRollbackOnly());
    agentSpan.setTag(SPRING_ISCOMPLETED, transactionStatus.isCompleted());
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
