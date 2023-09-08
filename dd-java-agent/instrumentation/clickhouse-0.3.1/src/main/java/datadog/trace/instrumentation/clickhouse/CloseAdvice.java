package datadog.trace.instrumentation.clickhouse;

import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;
import ru.yandex.clickhouse.ClickHouseConnectionImpl;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.clickhouse.ClickhouseDecorator.DECORATE;

public class CloseAdvice {
  public static final String DATABASE_CLOSECONNECTION = "clickhouse.close_connection";
  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope methodEnter(
      @Advice.This final ClickHouseConnectionImpl clickHouseConnection) {
    ContextStore<ClickHouseConnectionImpl, DBInfo> contextStore = InstrumentationContext.get(ClickHouseConnectionImpl.class, DBInfo.class);
    DBInfo dbInfo = contextStore.get(clickHouseConnection);
    if (dbInfo == null){
      dbInfo = DECORATE.parseDBInfo(null,clickHouseConnection,contextStore);
    }
    final AgentSpan span = startSpan(DATABASE_CLOSECONNECTION);
    DECORATE.afterStart(span);
    DECORATE.onConnection(span,dbInfo);
    span.setResourceName(DECORATE.spanNameForMethod(clickHouseConnection.getClass(), "close"));
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
