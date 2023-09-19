package datadog.trace.instrumentation.datasource;

import com.zaxxer.hikari.HikariDataSource;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;

public class HikariDataSourceDecorator extends DataSourceDecorator{
  public static final HikariDataSourceDecorator DECORATE = new HikariDataSourceDecorator();
  // todo 定时指标发送
  public void getPoolMetrics(AgentSpan span, HikariDataSource hikariDataSource) {
    span.setMetric("activeConnections",  (double) hikariDataSource.getHikariPoolMXBean().getActiveConnections());
    span.setMetric("totalConnections",  (double) hikariDataSource.getHikariPoolMXBean().getTotalConnections());
    span.setMetric("idleConnections",  (double) hikariDataSource.getHikariPoolMXBean().getIdleConnections());
    span.setMetric("threadsAwaitingConnection",  (double) hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
  }

  public void getConfigMetrics(AgentSpan span,HikariDataSource hikariDataSource) {
    span.setMetric("connectionTimeout",  (double) hikariDataSource.getConnectionTimeout());
    span.setMetric("validationTimeout",  (double) hikariDataSource.getValidationTimeout());
    span.setMetric("idleTimeout",  (double) hikariDataSource.getIdleTimeout());
    span.setMetric("leakDetectionThreshold",  (double) hikariDataSource.getLeakDetectionThreshold());
    span.setMetric("minimumIdle",  (double) hikariDataSource.getMinimumIdle());
    span.setMetric("maximumPoolSize",  (double) hikariDataSource.getMaximumPoolSize());
  }
}
