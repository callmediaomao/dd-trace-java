package datadog.trace.instrumentation.esjob;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.esjob.EsJob3Decorator.SCHEDULED_CALL;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class EsJob3Instrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "org.apache.shardingsphere.elasticjob.executor.ElasticJobExecutor";

  public EsJob3Instrumentation() {
    super("elasticjob");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".EsJob3Decorator",
    };
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod().and(isPrivate()).and(named("process"))
            .and(takesArguments(4))
            .and(takesArgument(1, named("org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts")))
            .and(takesArgument(2, int.class)),
        EsJob3Instrumentation.class.getName() + "$EsJobAdvice"
    );
  }

  public static class EsJobAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope onEnter(
        @Advice.Argument(1) final ShardingContexts shardingContexts,
        @Advice.Argument(2) final int item
        ) {
      try {
        AgentSpan span = startSpan(SCHEDULED_CALL);
        EsJob3Decorator.DECORATE.afterStart(span);
        EsJob3Decorator.DECORATE.onExecute(span, shardingContexts, item);
        AgentScope agentScope = activateSpan(span);
        return agentScope;
      } catch (Exception e) {
        return null;
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Enter final AgentScope scope,
        @Advice.Thrown final Throwable throwable) {
      if (scope == null) {
        return;
      }
      EsJob3Decorator.DECORATE.onError(scope, throwable);
      EsJob3Decorator.DECORATE.beforeFinish(scope);
      scope.close();
      scope.span().finish();
    }
  }
}
