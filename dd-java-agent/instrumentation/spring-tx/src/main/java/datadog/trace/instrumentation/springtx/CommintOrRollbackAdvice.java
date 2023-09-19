package datadog.trace.instrumentation.springtx;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommintOrRollbackAdvice {
  private static final Logger log = LoggerFactory.getLogger(CommintOrRollbackAdvice.class);

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope onEnter(
//      @Advice.Argument(0) final TransactionStatus transactionStatus
  ) {
    log.debug("------CommintOrRollbackAdvice!!!-----");
//    System.out.println("--------CommintOrRollbackAdvice!!!!----------");
//    AgentSpan agentSpan = startSpan("spring-tx");
//    DECORATE.afterStart(agentSpan);
//    DECORATE.spanNameForMethod(AbstractPlatformTransactionManager.class, "commit/rollback");
//    agentSpan.setTag(SPRING_ISNEWTRANSACTION, transactionStatus.isNewTransaction());
//    agentSpan.setTag(
//        SPRING_HASAVEPOINT, transactionStatus.hasSavepoint());
//    agentSpan.setTag(SPRING_ISROLLBACKONLY, transactionStatus.isRollbackOnly());
//    agentSpan.setTag(SPRING_ISCOMPLETED, transactionStatus.isCompleted());
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
