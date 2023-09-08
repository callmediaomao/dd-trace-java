package datadog.trace.instrumentation.clickhouse;

import datadog.trace.api.Config;
import datadog.trace.api.naming.SpanNaming;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.decorator.DBTypeProcessingDatabaseClientDecorator;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.ClickHouseConnectionImpl;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import java.lang.reflect.Field;

import static datadog.trace.bootstrap.instrumentation.jdbc.DBInfo.DEFAULT;

public class ClickhouseDecorator extends DBTypeProcessingDatabaseClientDecorator<DBInfo> {

  private static final Logger log = LoggerFactory.getLogger(ClickhouseDecorator.class);

  public static final ClickhouseDecorator DECORATE = new ClickhouseDecorator();
  private static final String DB_TYPE =
      SpanNaming.instance().namingSchema().database().normalizedName("clickhouse");

  @Override
  protected String[] instrumentationNames() {
    return new String[]{"clickhouse"};
  }

  @Override
  protected CharSequence spanType() {
    return InternalSpanTypes.CLICKHOUSE;
  }

  @Override
  protected CharSequence component() {
    return "clickhouse";
  }

  @Override
  protected String service() {
    return Config.get().getServiceName();
  }

  @Override
  protected String dbType() {
    return DB_TYPE;
  }

  @Override
  protected String dbUser(DBInfo dbInfo) {
    return dbInfo.getUser();
  }

  @Override
  protected String dbInstance(DBInfo dbInfo) {
    return dbInfo.getInstance();
  }

  @Override
  protected CharSequence dbHostname(DBInfo dbInfo) {
    return dbInfo.getHost();
  }

  /**
   *
   * @param clickHouseProperties
   * @param clickHouseConnection
   * @param contextStore
   */
  public DBInfo parseDBInfo(ClickHouseProperties clickHouseProperties, ClickHouseConnectionImpl clickHouseConnection, ContextStore<ClickHouseConnectionImpl, DBInfo> contextStore) {
    if (clickHouseProperties == null){
      clickHouseProperties = getField(clickHouseConnection);
    }
    // no instance and url
    DBInfo dbInfo = DEFAULT.toBuilder()
        .type("clickhouse")
        .subtype("clickhouse")
        .db(clickHouseProperties.getDatabase())
        .host(clickHouseProperties.getHost())
        .port(clickHouseProperties.getPort())
        .user(clickHouseProperties.getUser())
        .build();
    contextStore.put(clickHouseConnection,dbInfo);
    return dbInfo;
  }

  private ClickHouseProperties getField(ClickHouseConnectionImpl clickHouseConnection){
    try {
      Field properties = ClickHouseConnectionImpl.class.getField("properties");
      properties.setAccessible(true);
      return (ClickHouseProperties)properties.get(clickHouseConnection);
    } catch (Exception e) {
      log.debug("parse clickhouseProperties fail!!!!!");
    }
    return null;
  }
}
