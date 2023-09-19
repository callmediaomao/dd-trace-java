package datadog.trace.instrumentation.datasource;

public class DruidDataSourceInfo {
  private double maxActiveCount;
  private double activeCount;
  private double poolingCount;
  private double idleCount;
  private double lockQueueLength;
  private double maxWaitThreadCount;
  private double commitCount;
  private double connectCount;
  private double connectError;
  private double createError;

  public double getMaxActiveCount() {
    return maxActiveCount;
  }

  public void setMaxActiveCount(double maxActiveCount) {
    this.maxActiveCount = maxActiveCount;
  }

  public double getActiveCount() {
    return activeCount;
  }

  public void setActiveCount(double activeCount) {
    this.activeCount = activeCount;
  }

  public double getPoolingCount() {
    return poolingCount;
  }

  public void setPoolingCount(double poolingCount) {
    this.poolingCount = poolingCount;
  }

  public double getIdleCount() {
    return idleCount;
  }

  public void setIdleCount(double idleCount) {
    this.idleCount = idleCount;
  }

  public double getLockQueueLength() {
    return lockQueueLength;
  }

  public void setLockQueueLength(double lockQueueLength) {
    this.lockQueueLength = lockQueueLength;
  }

  public double getMaxWaitThreadCount() {
    return maxWaitThreadCount;
  }

  public void setMaxWaitThreadCount(double maxWaitThreadCount) {
    this.maxWaitThreadCount = maxWaitThreadCount;
  }

  public double getCommitCount() {
    return commitCount;
  }

  public void setCommitCount(double commitCount) {
    this.commitCount = commitCount;
  }

  public double getConnectCount() {
    return connectCount;
  }

  public void setConnectCount(double connectCount) {
    this.connectCount = connectCount;
  }

  public double getConnectError() {
    return connectError;
  }

  public void setConnectError(double connectError) {
    this.connectError = connectError;
  }

  public double getCreateError() {
    return createError;
  }

  public void setCreateError(double createError) {
    this.createError = createError;
  }

  public DruidDataSourceInfo(double maxActiveCount, double activeCount, double poolingCount, double idleCount, double lockQueueLength, double maxWaitThreadCount, double commitCount, double connectCount, double connectError, double createError) {
    this.maxActiveCount = maxActiveCount;
    this.activeCount = activeCount;
    this.poolingCount = poolingCount;
    this.idleCount = idleCount;
    this.lockQueueLength = lockQueueLength;
    this.maxWaitThreadCount = maxWaitThreadCount;
    this.commitCount = commitCount;
    this.connectCount = connectCount;
    this.connectError = connectError;
    this.createError = createError;
  }

  public DruidDataSourceInfo() {
  }

  public static class Builder {
    private double maxActiveCount;
    private double activeCount;
    private double poolingCount;
    private double idleCount;
    private double lockQueueLength;
    private double maxWaitThreadCount;
    private double commitCount;
    private double connectCount;
    private double connectError;
    private double createError;

    public Builder() {
    }

    public Builder(double maxActiveCount,
                   double activeCount,
                   double poolingCount,
                   double idleCount,
                   double lockQueueLength,
                   double maxWaitThreadCount,
                   double commitCount,
                   double connectCount,
                   double connectError,
                   double createError) {
      this.maxActiveCount = maxActiveCount;
      this.activeCount = activeCount;
      this.poolingCount = poolingCount;
      this.idleCount = idleCount;
      this.lockQueueLength = lockQueueLength;
      this.maxWaitThreadCount = maxWaitThreadCount;
      this.commitCount = commitCount;
      this.connectCount = connectCount;
      this.connectError = connectError;
      this.createError = createError;
    }

    public DruidDataSourceInfo.Builder maxActiveCount(double maxActiveCount) {
      this.maxActiveCount = maxActiveCount;
      return this;
    }

    public DruidDataSourceInfo.Builder activeCount(double activeCount) {
      this.activeCount = activeCount;
      return this;
    }

    public DruidDataSourceInfo.Builder poolingCount(double poolingCount) {
      this.poolingCount = poolingCount;
      return this;
    }

    public DruidDataSourceInfo.Builder maxWaitThreadCount( double maxWaitThreadCount) {
      this.maxWaitThreadCount = maxWaitThreadCount;
      return this;
    }

    public DruidDataSourceInfo.Builder lockQueueLength(double lockQueueLength) {
      this.lockQueueLength = lockQueueLength;
      return this;
    }

    public DruidDataSourceInfo.Builder idleCount(double idleCount) {
      this.idleCount = idleCount;
      return this;
    }

    public DruidDataSourceInfo.Builder commitCount(double commitCount) {
      this.commitCount = commitCount;
      return this;
    }

    public DruidDataSourceInfo.Builder connectCount(double connectCount) {
      this.connectCount = connectCount;
      return this;
    }

    public DruidDataSourceInfo.Builder connectError(double connectError) {
      this.connectError = connectError;
      return this;
    }

    public DruidDataSourceInfo.Builder createError(double createError) {
      this.createError = createError;
      return this;
    }

    public DruidDataSourceInfo build() {
      return new DruidDataSourceInfo(maxActiveCount, activeCount, poolingCount,
          idleCount, lockQueueLength, maxWaitThreadCount, commitCount, connectCount,
          connectError, createError);
    }
  }

}
