package datadog.trace.instrumentation.datasource;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;


@AutoService(Instrumenter.class)
public class DruidCloseInstrumentation extends Instrumenter.Tracing
  implements Instrumenter.ForSingleType {

  private static final Logger log = LoggerFactory.getLogger(DruidGetConInstrumentation.class);

  public DruidCloseInstrumentation() {
    super("druidDatasource","datasource");
  }

  public String[] helperClassNames() {
    return new String[]{
        packageName+".DruidDataSourceDecorator",
    };
  }

  public Map<String, String> contextStore() {
    return Collections.singletonMap("java.sql.Connection", DBInfo.class.getName());
  }

  public void adviceTransformations(AdviceTransformation transformation) {

    transformation.applyAdvice(
        isMethod()
            .and(isPublic())
            .and(named("close"))
            .and(takesNoArguments()),
        DruidCloseInstrumentation.class.getName() + "$DataSourceAdvice");
  }

  public String instrumentedType() {
    return "com.alibaba.druid.pool.DruidPooledConnection";
  }

  public static class DataSourceAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope onEnter(
        @Advice.This final Connection connection
    ) {
      try {
        AgentSpan span = startSpan("datasource", "datasource.close");
        DataSourceDecorator.DECORATE.afterStart(span);
        DataSourceDecorator.DECORATE.onConnection(span,connection, InstrumentationContext.get(Connection.class, DBInfo.class));
        AgentScope agentScope = activateSpan(span);
        span.setTag(Tags.COMPONENT, DataSourceConstant.DRUID);
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
      DataSourceDecorator.DECORATE.onError(scope, throwable);
      DataSourceDecorator.DECORATE.beforeFinish(scope);
      scope.close();
      scope.span().finish();
    }
  }

}
