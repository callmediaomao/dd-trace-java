package datadog.trace.instrumentation.mongo2;

import datadog.trace.api.Config;
import datadog.trace.api.naming.SpanNaming;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.ClientDecorator;

public class MongoDecorator
    extends ClientDecorator {
  private static final String DB_TYPE =
      SpanNaming.instance().namingSchema().database().normalizedName("mongo");
  private static final String SERVICE_NAME =
      SpanNaming.instance()
          .namingSchema()
          .database()
          .service(Config.get().getServiceName(), DB_TYPE);
  public static final UTF8BytesString OPERATION_NAME =
      UTF8BytesString.create(SpanNaming.instance().namingSchema().database().operation(DB_TYPE));

  public static final MongoDecorator DECORATOR = new MongoDecorator();

  @Override
  protected final String[] instrumentationNames() {
    return new String[]{"mongo"};
  }

  @Override
  protected final String service() {
    return SERVICE_NAME;
  }

  @Override
  protected final CharSequence component() {
    return "java-mongo";
  }

  @Override
  protected final CharSequence spanType() {
    return InternalSpanTypes.MONGO;
  }

}
