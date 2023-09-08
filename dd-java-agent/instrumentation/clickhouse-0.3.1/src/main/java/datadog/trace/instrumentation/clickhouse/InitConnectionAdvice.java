package datadog.trace.instrumentation.clickhouse;

import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;
import ru.yandex.clickhouse.ClickHouseConnectionImpl;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.clickhouse.ClickhouseDecorator.DECORATE;

public class InitConnectionAdvice {

  public static final String DATABASE_INITCONNECTION = "clickhouse.init_connection";
  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope methodEnter(@Advice.Argument(0) final ClickHouseProperties clickHouseProperties,
                                       @Advice.This final ClickHouseConnectionImpl clickHouseConnection) {
    ContextStore<ClickHouseConnectionImpl, DBInfo> contextStore = InstrumentationContext.get(ClickHouseConnectionImpl.class, DBInfo.class);
    final AgentSpan span = startSpan(DATABASE_INITCONNECTION);
    DBInfo dbInfo = DECORATE.parseDBInfo(clickHouseProperties, clickHouseConnection, contextStore);
    DECORATE.afterStart(span);
    DECORATE.onConnection(span,dbInfo);
    span.setResourceName(DECORATE.spanNameForMethod(clickHouseConnection.getClass(), "initConnection"));
    final AgentScope agentScope = activateSpan(span);
    agentScope.setAsyncPropagation(true);
    return agentScope;
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void methodExit(
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
