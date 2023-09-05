package datadog.trace.instrumentation.datasource;

import datadog.trace.api.Config;
import datadog.trace.api.naming.NamingSchema;
import datadog.trace.api.naming.SpanNaming;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;

public class NamingEntry {
  private final String service;
  private final CharSequence operation;

  private final String dbType;

  public NamingEntry(String rawDbType) {
    final NamingSchema.ForDatabase schema = SpanNaming.instance().namingSchema().database();
    this.dbType = schema.normalizedName(rawDbType);
    this.service = schema.service(Config.get().getServiceName(), dbType);
    this.operation = UTF8BytesString.create(schema.operation(dbType));
  }

  public String getService() {
    return service;
  }

  public CharSequence getOperation() {
    return operation;
  }

  public String getDbType() {
    return dbType;
  }
}
