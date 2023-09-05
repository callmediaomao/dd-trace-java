package datadog.trace.instrumentation.esjob;

import com.dangdang.ddframe.job.executor.ShardingContexts;
import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.extendsClass;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.esjob.EsJob2Decorator.SCHEDULED_CALL;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class EsJob2Instrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForTypeHierarchy {

  private static final String ENHANCE_CLASS = "com.dangdang.ddframe.job.executor.AbstractElasticJobExecutor";

  public EsJob2Instrumentation() {
    super("elasticjob");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".EsJobDecorator",
    };
  }

//  @Override
//  public String instrumentedType() {
//    return ENHANCE_CLASS;
//  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(isPrivate())
            .and(named("process"))
            .and(takesArguments(3))
            .and(takesArgument(0, named("com.dangdang.ddframe.job.executor.ShardingContexts"))
                .and(takesArgument(1, int.class))),
        EsJob2Instrumentation.class.getName() + "$EsJobAdvice"
    );
  }

  @Override
  public String hierarchyMarkerType() {
    return ENHANCE_CLASS;
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return extendsClass(named(hierarchyMarkerType()));
  }

  public static class EsJobAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope onEnter(
        @Advice.Argument(0) final ShardingContexts shardingContexts,
        @Advice.Argument(1) final int item
    ) {
      try {
        AgentSpan span = startSpan(SCHEDULED_CALL);
        EsJob2Decorator.DECORATE.afterStart(span);
        EsJob2Decorator.DECORATE.onExecute(span,shardingContexts,item);
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
      EsJob2Decorator.DECORATE.onError(scope, throwable);
      EsJob2Decorator.DECORATE.beforeFinish(scope);
      scope.close();
      scope.span().finish();
    }
  }
}
