package datadog.trace.instrumentation.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import net.bytebuddy.asm.Advice;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;

public class DataSourceAdvice {

  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  public static void stopSpan(
      @Advice.Argument(0) final Object druidDataSource,
      @Advice.Thrown final Throwable throwable) {
    // todo 定时塞指标
    AgentSpan span = startSpan("datasource", "datasource.addDataSource");
    DataSourceDecorator.DECORATE.afterStart(span);
    span.setTag(Tags.COMPONENT, DataSourceConstant.DRUID);
    DruidDataSourceDecorator.DECORATE.getMetrics(span,(DruidDataSource) druidDataSource);
    AgentScope scope = activateSpan(span);
    DataSourceDecorator.DECORATE.onError(scope, throwable);
    DataSourceDecorator.DECORATE.beforeFinish(scope);
    scope.close();
    scope.span().finish();
  }

}
