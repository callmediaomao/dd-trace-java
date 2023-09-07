package datadog.trace.instrumentation.dubbo_2_7x;

public class DubboInfo {
  public static final  String CORE_POOL_SIZE = "core_pool_size";
  private Integer corePoolSize;


  public static final String MAX_POOL_SIZE = "max_pool_size";
  private Integer maximumPoolSize;

  public static final String LARGEST_POOL_SIZE = "largest_pool_size";
  private Integer largestPoolSize;

  public static final String POOL_SIZE = "pool_size";
  private Integer poolSize;

  public static final String QUEUE_SIZE = "queue_size";
  private Integer queueSize;

  public static final String ACTIVE_SIZE = "active_size";
  private Integer activeCount;

  public static final String TASK_COUNT = "task_count";
  private Long taskCount;

  public static final String COMPLETED_TASK_COUNT = "completed_task_count";
  private Long completedTaskCount;

  public Integer getCorePoolSize() {
    return corePoolSize;
  }

  public void setCorePoolSize(Integer corePoolSize) {
    this.corePoolSize = corePoolSize;
  }

  public Integer getMaximumPoolSize() {
    return maximumPoolSize;
  }

  public void setMaximumPoolSize(Integer maximumPoolSize) {
    this.maximumPoolSize = maximumPoolSize;
  }

  public Integer getLargestPoolSize() {
    return largestPoolSize;
  }

  public void setLargestPoolSize(Integer largestPoolSize) {
    this.largestPoolSize = largestPoolSize;
  }

  public Integer getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(Integer poolSize) {
    this.poolSize = poolSize;
  }

  public Integer getQueueSize() {
    return queueSize;
  }

  public void setQueueSize(Integer queueSize) {
    this.queueSize = queueSize;
  }

  public Integer getActiveCount() {
    return activeCount;
  }

  public void setActiveCount(Integer activeCount) {
    this.activeCount = activeCount;
  }

  public Long getTaskCount() {
    return taskCount;
  }

  public void setTaskCount(Long taskCount) {
    this.taskCount = taskCount;
  }

  public Long getCompletedTaskCount() {
    return completedTaskCount;
  }

  public void setCompletedTaskCount(Long completedTaskCount) {
    this.completedTaskCount = completedTaskCount;
  }

  private String threadPoolName;

  public String getThreadPoolName() {
    return threadPoolName;
  }

  public void setThreadPoolName(String threadName) {
    this.threadPoolName = threadName;
  }
}
