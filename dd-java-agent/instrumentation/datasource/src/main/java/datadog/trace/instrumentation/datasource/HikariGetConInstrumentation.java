package datadog.trace.instrumentation.datasource;

import com.google.auto.service.AutoService;
import com.zaxxer.hikari.HikariDataSource;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static net.bytebuddy.matcher.ElementMatchers.*;


@AutoService(Instrumenter.class)
public class HikariGetConInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {


  public HikariGetConInstrumentation() {
    super("hikariDatasource","datasource");
  }

  public String instrumentedType() {
    return "com.zaxxer.hikari.HikariDataSource";
  }

  public String[] helperClassNames() {
    return new String[]{
//        packageName+".DataSourceDecorator",
        packageName+".HikariDataSourceDecorator",
    };
  }

  public Map<String, String> contextStore() {
    return Collections.singletonMap("java.sql.Connection", DBInfo.class.getName());
  }

  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(isPublic())
            .and(named("getConnection"))
            .and(takesNoArguments()),
        HikariGetConInstrumentation.class.getName() + "$DataSourceAdvice");
  }

  public static class DataSourceAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.This final HikariDataSource hikariDataSource,
        @Advice.Return final Connection connection,
        @Advice.Thrown final Throwable throwable) {

      AgentSpan span = startSpan("datasource", "datasource.getConnection");
      HikariDataSourceDecorator.DECORATE.afterStart(span);
      HikariDataSourceDecorator.DECORATE.onConnection(span,connection, InstrumentationContext.get(Connection.class,DBInfo.class));
      span.setSpanType("datasource");
      AgentScope scope = activateSpan(span);
      HikariDataSourceDecorator.DECORATE.getConfigMetrics(span,hikariDataSource);
      HikariDataSourceDecorator.DECORATE.getPoolMetrics(span,hikariDataSource);
      span.setTag(Tags.COMPONENT, DataSourceConstant.HIKARI);
      HikariDataSourceDecorator.DECORATE.onError(scope, throwable);
      HikariDataSourceDecorator.DECORATE.beforeFinish(scope);
      scope.close();
      scope.span().finish();
    }


  }
}
