package datadog.trace.instrumentation.esjob;

import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.BaseDecorator;
import org.apache.shardingsphere.elasticjob.executor.ElasticJobExecutor;
import org.apache.shardingsphere.elasticjob.infra.listener.ShardingContexts;
import org.apache.shardingsphere.elasticjob.tracing.event.JobExecutionEvent;

import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.*;

public class EsJob3Decorator extends BaseDecorator {

  public static final CharSequence SCHEDULED_CALL = UTF8BytesString.create("scheduled.call");
  public static final EsJob3Decorator DECORATE = new EsJob3Decorator();

  private EsJob3Decorator() {}

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"elastic-job"};
  }

  @Override
  protected CharSequence spanType() {
    return null;
  }

  @Override
  protected CharSequence component() {
    return "elastic-job";
  }

  public void onExecute(AgentSpan span, ShardingContexts shardingContexts,
                        Integer item) {
    if (shardingContexts != null){
      span.setResourceName(spanNameForMethod(ElasticJobExecutor.class,"process"));
      span.setTag(JOB_ITEM,item);
      span.setTag(JOB_NAME,shardingContexts.getJobName());
      span.setTag(JOB_TASK_ID,shardingContexts.getTaskId());
      span.setTag(JOB_SHARDING_TOTAL_COUNT,Integer.toString(shardingContexts.getShardingTotalCount()));
      span.setTag(JOB_SHARDING_ITEM_PARAMETERS,shardingContexts.getShardingItemParameters() == null ? "" : shardingContexts.getShardingItemParameters().toString());
    }
  }
}
