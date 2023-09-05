package datadog.trace.instrumentation.esjob;

import com.dangdang.ddframe.job.executor.AbstractElasticJobExecutor;
import com.dangdang.ddframe.job.executor.ShardingContexts;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.BaseDecorator;

import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.*;

public class EsJob2Decorator extends BaseDecorator {

  public static final CharSequence SCHEDULED_CALL = UTF8BytesString.create("scheduled.call");
  public static final EsJob2Decorator DECORATE = new EsJob2Decorator();

  private EsJob2Decorator() {}

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
      span.setResourceName(spanNameForMethod(AbstractElasticJobExecutor.class,"process"));
      span.setTag(JOB_ITEM,item);
      span.setTag(JOB_NAME,shardingContexts.getJobName());
      span.setTag(JOB_TASK_ID,shardingContexts.getTaskId());
      span.setTag(JOB_SHARDING_TOTAL_COUNT,Integer.toString(shardingContexts.getShardingTotalCount()));
      span.setTag(JOB_SHARDING_ITEM_PARAMETERS,shardingContexts.getShardingItemParameters() == null ? "" : shardingContexts.getShardingItemParameters().toString());
    }
  }
}
