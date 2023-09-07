package datadog.trace.instrumentation.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidDataSourceStatManager;
import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;


/**
 * @Author: FanGang
 * @Date: 2023/8/30 14:41
 */
@AutoService(Instrumenter.class)
public class DruidAddDataSourceInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  public DruidAddDataSourceInstrumentation() {
    super("druidDatasource","datasource");
  }

  public String instrumentedType() {
    return "com.alibaba.druid.stat.DruidDataSourceStatManager";
  }

  public String[] helperClassNames() {
    return new String[]{
        packageName + ".DataSourceDecorator",
    };
  }

  public Map<String, String> contextStore() {
    return Collections.singletonMap("java.sql.Connection", DBInfo.class.getName());
  }

  public void adviceTransformations(AdviceTransformation transformation) {
    // 静态方法
    transformation.applyAdvice(
        isMethod()
            .and(isStatic())
            .and(named("addDataSource"))
            .and(takesArguments(2).and(takesArgument(0, Object.class)))
            .and(returns(named("javax.management.ObjectName"))),

        DruidAddDataSourceInstrumentation.class.getName() + "$DataSourceAdvice"
    );
  }

  public static class DataSourceAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope onEnter(
        @Advice.This final DruidDataSourceStatManager ds,
        @Advice.Argument(0) final Object druidDataSource
    ) {
      try {
        // todo 此处应该直接拿connection而不是通过datasource去获取
        AgentSpan span = startSpan("datasource", "datasource.addDataSource");
        DataSourceDecorator.DECORATE.afterStart(span);
        Connection connection = ((DruidDataSource)druidDataSource).getConnection();
        DataSourceDecorator.DECORATE.onConnection(span,connection, InstrumentationContext.get(Connection.class, DBInfo.class));
        AgentScope agentScope = activateSpan(span);
        connection.close();
        return agentScope;
      } catch (SQLException e) {
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
