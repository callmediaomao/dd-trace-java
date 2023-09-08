package datadog.trace.instrumentation.clickhouse;

import datadog.trace.bootstrap.CallDepthThreadLocalMap;
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

public class PrepareStatementAdvice {

  public static final String PREPARE_STATEMENT = "clickhouse.prepare_statement";

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope onEnter(@Advice.This final ClickHouseConnectionImpl clickHouseConnection,
                                   @Advice.Argument(0) String sql) {
    int depth = CallDepthThreadLocalMap.incrementCallDepth(ClickHouseConnectionImpl.class);
    if (depth > 0) {
      return null;
    }
    try {
      ContextStore<ClickHouseConnectionImpl, DBInfo> contextStore = InstrumentationContext.get(ClickHouseConnectionImpl.class, DBInfo.class);
      DBInfo dbInfo = contextStore.get(clickHouseConnection);
      if (dbInfo == null){
        dbInfo = DECORATE.parseDBInfo(null,clickHouseConnection,contextStore);
      }
      final AgentSpan span = startSpan(PREPARE_STATEMENT);
      DECORATE.afterStart(span);
      DECORATE.onConnection(
          span, dbInfo);
      DECORATE.onStatement(span, sql);
      return activateSpan(span);
    } catch (Exception e) {
      // if we can't get the connection for any reason
      return null;
    }
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
    CallDepthThreadLocalMap.reset(ClickHouseConnectionImpl.class);
  }
}
