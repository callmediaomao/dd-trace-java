package datadog.trace.instrumentation.clickhouse;

import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import net.bytebuddy.asm.Advice;
import ru.yandex.clickhouse.ClickHouseConnectionImpl;
import ru.yandex.clickhouse.ClickHouseStatement;

import static datadog.trace.instrumentation.clickhouse.ClickhouseDecorator.DECORATE;

public class CreateStatementAdvice {

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void stopSpan(
      @Advice.This final ClickHouseConnectionImpl clickHouseConnection,
      @Advice.Return(readOnly = false) ClickHouseStatement clickHouseStatement) {
    ContextStore<ClickHouseConnectionImpl, DBInfo> contextStore = InstrumentationContext.get(ClickHouseConnectionImpl.class, DBInfo.class);
    DBInfo dbInfo = contextStore.get(clickHouseConnection);
    if (dbInfo == null){
      dbInfo = DECORATE.parseDBInfo(null,clickHouseConnection,contextStore);
    }
    clickHouseStatement = new TracedClickHouseStatement(clickHouseStatement,dbInfo);
  }
}
