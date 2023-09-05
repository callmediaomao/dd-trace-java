package datadog.trace.instrumentation.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.CallDepthThreadLocalMap;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;


@AutoService(Instrumenter.class)
public class DruidGetConInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  public DruidGetConInstrumentation() {
    super("druidDatasource","datasource");
  }

  public String instrumentedType() {
    return "com.alibaba.druid.pool.DruidDataSource";
  }

  public String[] helperClassNames() {
    return new String[]{
        packageName+".DataSourceDecorator",
        packageName+".NamingEntry",
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
            .and(takesNoArguments())
            .and(returns(named("com.alibaba.druid.pool.DruidPooledConnection"))),
        DruidGetConInstrumentation.class.getName() + "$DataSourceAdvice");
  }

  public static class DataSourceAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Return final DruidPooledConnection connection,
        @Advice.Thrown final Throwable throwable) {
      AgentSpan span = startSpan("datasource", "datasource.getConnection");
      DataSourceDecorator.DECORATE.afterStart(span);
      DataSourceDecorator.DECORATE.onConnection(span,connection, InstrumentationContext.get(Connection.class,DBInfo.class));
      span.setSpanType("datasource");
      span.setTag(Tags.COMPONENT, DataSourceConstant.DRUID);
      AgentScope scope = activateSpan(span);

      DataSourceDecorator.DECORATE.onError(scope, throwable);
      DataSourceDecorator.DECORATE.beforeFinish(scope);
      scope.close();
      scope.span().finish();
    }
  }
}
