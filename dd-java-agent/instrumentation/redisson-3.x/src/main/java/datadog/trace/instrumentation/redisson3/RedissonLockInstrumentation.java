package datadog.trace.instrumentation.redisson3;


import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import org.redisson.RedissonLock;
import org.redisson.client.protocol.RedisCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.redisson3.Redisson3ClientDecorator.DECORATE;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class RedissonLockInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final Logger log = LoggerFactory.getLogger(RedissonLockInstrumentation.class);


  private static final String REDISSON_LOCK_CLASS = "org.redisson.RedissonLock";

  public RedissonLockInstrumentation() {
    super("redisson", "redis");
  }


  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".Redisson3ClientDecorator",
    };
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(named("tryLockInnerAsync"))
            .and(takesArguments(4))
            .and(takesArgument(0, long.class)
                .and(takesArgument(1, named("java.util.concurrent.TimeUnit")))
                .and(takesArgument(2, long.class))
                .and(takesArgument(3, named("org.redisson.client.protocol.RedisStrictCommand")))),
        RedissonLockInstrumentation.class.getName() + "$RedissonLockAdvice"
    );
  }

  @Override
  public String instrumentedType() {
    return REDISSON_LOCK_CLASS;
  }

  public static class RedissonLockAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope onEnter(
        @Advice.Argument(0) final long leaseTime,
        @Advice.This final RedissonLock lock,
        @Advice.Argument(2) final long threadId,
        @Advice.Argument(3) final RedisCommand redisCommand
    ) {
      final AgentSpan span = startSpan(Redisson3ClientDecorator.OPERATION_NAME);
      DECORATE.afterStart(span, leaseTime, threadId, lock);
      DECORATE.onStatement(span, redisCommand.getName());
      return activateSpan(span);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Enter final AgentScope scope, @Advice.Thrown final Throwable throwable) {
      if (scope == null) {
        return;
      }
      DECORATE.onError(scope.span(), throwable);
      DECORATE.beforeFinish(scope.span());
      scope.close();
      scope.span().finish();
    }
  }

}
