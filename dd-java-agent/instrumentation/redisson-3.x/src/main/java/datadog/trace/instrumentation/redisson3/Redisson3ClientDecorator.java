package datadog.trace.instrumentation.redisson3;

import datadog.trace.api.Config;
import datadog.trace.api.naming.SpanNaming;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.InternalSpanTypes;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.DBTypeProcessingDatabaseClientDecorator;
import jodd.util.StringUtil;
import org.redisson.RedissonLock;
import org.redisson.client.protocol.CommandData;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collection;

import static datadog.trace.api.DDTags.THREAD_ID;
import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.LEASE_TIME;
import static datadog.trace.bootstrap.instrumentation.api.InstrumentationTags.LOCK_NAME;

public class Redisson3ClientDecorator
    extends DBTypeProcessingDatabaseClientDecorator<CommandData<?, ?>> {
  public static final Redisson3ClientDecorator DECORATE = new Redisson3ClientDecorator();

  public static final CharSequence OPERATION_NAME =
      UTF8BytesString.create(SpanNaming.instance().namingSchema().cache().operation("redisson"));
  private static final String SERVICE_NAME =
      SpanNaming.instance().namingSchema().cache().service(Config.get().getServiceName(), "redis");

  private static final CharSequence COMPONENT_NAME = UTF8BytesString.create("redisson");


  public AgentSpan afterStart(AgentSpan span, long leaseTime, long threadId, RedissonLock lock) {
    span.setTag(THREAD_ID, threadId);
    span.setTag(LEASE_TIME, leaseTime);
    span.setTag(LOCK_NAME, lock.getName());
    return super.afterStart(span);
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[]{"redisson", "redis"};
  }

  @Override
  protected String service() {
    return SERVICE_NAME;
  }

  @Override
  protected CharSequence component() {
    return COMPONENT_NAME;
  }

  @Override
  protected CharSequence spanType() {
    return InternalSpanTypes.REDIS;
  }

  @Override
  protected String dbType() {
    return "redis";
  }

  @Override
  protected String dbUser(CommandData<?, ?> commandData) {
    return null;
  }

  @Override
  protected String dbInstance(CommandData<?, ?> commandData) {
    return null;
  }

  @Override
  protected CharSequence dbHostname(CommandData<?, ?> commandData) {
    return null;
  }

  public String getPeer(Object obj) {
    if (obj instanceof String) {
      return ((String) obj).replace("redis://", "");
    } else if (obj instanceof URI) {
      URI uri = (URI) obj;
      return uri.getHost() + ":" + uri.getPort();
    } else {
      return null;
    }
  }

  public Object getObjectField(Object obj, String name) throws NoSuchFieldException, IllegalAccessException {
    Field field = obj.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(obj);
  }

  public void appendAddresses(StringBuilder peer, Collection nodeAddresses) {
    if (nodeAddresses != null && !nodeAddresses.isEmpty()) {
      for (Object uri : nodeAddresses) {
        peer.append(getPeer(uri)).append(";");
      }
    }
  }

  private final String ABBR = "...";

  public String shorten(String original) {
    if (!StringUtil.isEmpty(original) && original.length() > 200) {
      return original.substring(0, 200 - 3) + ABBR;
    }
    return original;
  }
}
