package datadog.trace.instrumentation.springtx;

import datadog.trace.api.Config;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.decorator.ClientDecorator;

public class SpringTxDecorator extends ClientDecorator {

  public static final SpringTxDecorator DECORATE =
      new SpringTxDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[]{
        "spring-tx"
    };
  }

  @Override
  protected CharSequence spanType() {
    return InternalSpanTypes.SPRING_TX;
  }

  @Override
  protected CharSequence component() {
    return "spring-tx";
  }

  @Override
  protected String service() {
    return Config.get().getServiceName();
  }
}
