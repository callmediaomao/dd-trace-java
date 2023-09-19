package datadog.trace.instrumentation.datasource;

import datadog.trace.api.Config;
import datadog.trace.api.naming.SpanNaming;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.ClientDecorator;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;
import datadog.trace.bootstrap.instrumentation.jdbc.JDBCConnectionUrlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static datadog.trace.bootstrap.instrumentation.api.Tags.PEER_PORT;

public class DataSourceDecorator extends ClientDecorator {

  private static final Logger log = LoggerFactory.getLogger(DataSourceDecorator.class);

  public static final DataSourceDecorator DECORATE = new DataSourceDecorator();

  public static final String DATASOURCE = "datasource";

  public static final CharSequence DATASOURCE_CHAR = UTF8BytesString.create(DATASOURCE);

  public static final CharSequence OPERATION_NAME =
      UTF8BytesString.create(SpanNaming.instance().namingSchema().cache().operation(DATASOURCE));

  /**
   * 自定义成下游需要
   * @return
   */
  protected String spanKind() {
    return Tags.SPAN_KIND_SERVER;
  }

  protected String[] instrumentationNames() {
    return new String[]{"druidDatasource"};
  }

  protected CharSequence spanType() {
    return InternalSpanTypes.DATASOURCE;
  }

  protected CharSequence component() {
    return DATASOURCE;
  }


  protected String service() {
    // 能够通过 onConnection 覆盖
    return DATASOURCE;
  }

  protected String dbType() {
    return DATASOURCE;
  }

  protected String dbUser(DBInfo info) {
    return info.getUser();
  }

  protected String dbInstance(DBInfo info) {
    return info.getInstance();
  }

  protected CharSequence dbHostname(DBInfo info) {
    return info.getHost();
  }

  public AgentSpan onConnection(
      final AgentSpan span,
      final Connection connection,
      ContextStore<Connection, DBInfo> contextStore) {

    final DBInfo dbInfo = parseDBInfo(connection, contextStore);
    if (dbInfo != null) {
      span.setTag(PEER_PORT,dbInfo.getPort());

      if (connection != null) {
        span.setTag(Tags.DB_USER, dbUser(dbInfo));
        final String instanceName = dbInstance(dbInfo);
        span.setTag(Tags.DB_INSTANCE, instanceName);

//        String serviceName = dbClientService(instanceName);
//        if (null != serviceName) {
//          span.setServiceName(serviceName);
//        }else {
//        }
        // service 设置为变量中的service 下游需要
        String name = Config.get().getServiceName();
        span.setServiceName(name);

        CharSequence hostName = dbHostname(dbInfo);
        if (hostName != null) {
          span.setTag(Tags.PEER_HOSTNAME, hostName);
        }
      }
    }
    return span;
  }

  public String dbClientService(final String instanceName) {
    String service = null;
    if (instanceName != null && Config.get().isDbClientSplitByInstance()) {
      service =
          Config.get().isDbClientSplitByInstanceTypeSuffix()
              ? instanceName + "-" + dbType()
              : instanceName;
    }
    return service;
  }
  public static DBInfo parseDBInfo(
      final Connection connection, ContextStore<Connection, DBInfo> contextStore) {
    DBInfo dbInfo = contextStore.get(connection);
    /*
     * Logic to get the DBInfo from a JDBC Connection, if the connection was not created via
     * Driver.connect, or it has never seen before, the connectionInfo map will return null and will
     * attempt to extract DBInfo from the connection. If the DBInfo can't be extracted, then the
     * connection will be stored with the DEFAULT DBInfo as the value in the connectionInfo map to
     * avoid retry overhead.
     */
    {
      if (dbInfo == null) {
        // first look for injected DBInfo in wrapped delegates
        Connection conn = connection;
        Set<Connection> connections = new HashSet<>();
        connections.add(conn);
        try {
          while (dbInfo == null) {
            Connection delegate = conn.unwrap(Connection.class);
            if (delegate == null || !connections.add(delegate)) {
              // cycle detected, stop looking
              break;
            }
            dbInfo = contextStore.get(delegate);
            conn = delegate;
          }
        } catch (Throwable ignore) {
        }
        if (dbInfo == null) {
          // couldn't find DBInfo anywhere, so fall back to default
          dbInfo = parseDBInfoFromConnection(connection);
        }
        // store the DBInfo on the outermost connection instance to avoid future searches
        contextStore.put(connection, dbInfo);
      }
    }
    return dbInfo;
  }

  public static DBInfo parseDBInfoFromConnection(final Connection connection) {
    DBInfo dbInfo;
    try {
      final DatabaseMetaData metaData = connection.getMetaData();
      final String url = metaData.getURL();
      if (url != null) {
        try {
          dbInfo = JDBCConnectionUrlParser.extractDBInfo(url, connection.getClientInfo());
        } catch (final Throwable ex) {
          // getClientInfo is likely not allowed.
          dbInfo = JDBCConnectionUrlParser.extractDBInfo(url, null);
        }
      } else {
        dbInfo = DBInfo.DEFAULT;
      }
    } catch (final SQLException se) {
      dbInfo = DBInfo.DEFAULT;
    }
    return dbInfo;
  }
}
