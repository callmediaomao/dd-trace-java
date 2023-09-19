package datadog.trace.instrumentation.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;

public class DruidDataSourceDecorator extends DataSourceDecorator{
  public static final DruidDataSourceDecorator DECORATE = new DruidDataSourceDecorator();
  // todo 定时指标发送
  public  void getMetrics(AgentSpan span, DruidDataSource dataSource) {
    span.setMetric("maxActiveCount",   dataSource.getMaxActive());
    span.setMetric("activeCount",   dataSource.getActiveCount());
    span.setMetric("poolingCount",   dataSource.getPoolingCount());
    span.setMetric("idleCount",   dataSource.getPoolingCount()-dataSource.getActiveCount());
    span.setMetric("lockQueueLength",   dataSource.getLockQueueLength());
    span.setMetric("maxWaitThreadCount",   dataSource.getMaxWaitThreadCount());
    span.setMetric("commitCount",   dataSource.getCommitCount());
    span.setMetric("connectCount",   dataSource.getConnectCount());
    span.setMetric("connectError",   dataSource.getConnectErrorCount());
    span.setMetric("createError",   dataSource.getCreateErrorCount());

    
    /*DruidDataSourceInfo.Builder builder = new DruidDataSourceInfo.Builder();
    return builder
        .maxActiveCount((double) druidDataSource.getMaxActive())
        .activeCount((double) druidDataSource.getActiveCount())
        .poolingCount((double) druidDataSource.getPoolingCount())
        .idleCount((double) (druidDataSource.getPoolingCount() - druidDataSource.getActiveCount()))
        .lockQueueLength((double) druidDataSource.getLockQueueLength())
        .maxWaitThreadCount((double) druidDataSource.getMaxWaitThreadCount())
        .commitCount((double) druidDataSource.getCommitCount())
        .connectCount((double) druidDataSource.getConnectCount())
        .connectError((double) druidDataSource.getConnectErrorCount())
        .createError((double) druidDataSource.getCreateErrorCount())
        .build();*/
  }
}
