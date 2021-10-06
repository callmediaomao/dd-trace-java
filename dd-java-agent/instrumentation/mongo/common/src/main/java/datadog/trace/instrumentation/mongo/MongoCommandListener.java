package datadog.trace.instrumentation.mongo;

import static datadog.trace.api.Functions.UTF8_ENCODE;
import static datadog.trace.api.cache.RadixTreeCache.PORTS;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;

import com.mongodb.ServerAddress;
import com.mongodb.connection.ConnectionDescription;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import datadog.trace.api.Config;
import datadog.trace.api.cache.DDCache;
import datadog.trace.api.cache.DDCaches;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.Tags;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bson.BsonDocument;
import org.bson.ByteBuf;

public final class MongoCommandListener implements CommandListener {
  public static final class SpanEntry {
    public final AgentSpan span;
    public volatile boolean suspended = false;

    public SpanEntry(AgentSpan span) {
      this.span = span;
    }

    @Override
    public String toString() {
      return "SpanEntry{" + "span=" + span + ", suspended=" + suspended + '}';
    }
  }

  private static final DDCache<String, UTF8BytesString> COMMAND_NAMES =
      DDCaches.newUnboundedCache(16);

  private final Map<Integer, SpanEntry> spanMap = new ConcurrentHashMap<>();
  private final ContextStore<BsonDocument, ByteBuf> byteBufAccessor;
  private final MongoDecorator decorator;
  private final ContextStore<ConnectionDescription, CommandListener> listenerAccessor;
  private volatile String applicationName;
  private final int priority;

  public MongoCommandListener(int priority, MongoDecorator decorator) {
    this(priority, decorator, null, null);
  }

  public MongoCommandListener(
      int priority,
      MongoDecorator decorator,
      ContextStore<BsonDocument, ByteBuf> byteBufAccessor,
      ContextStore<ConnectionDescription, CommandListener> listenerAccessor) {
    this.priority = priority;
    this.decorator = decorator;
    this.byteBufAccessor = byteBufAccessor;
    this.listenerAccessor = listenerAccessor;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * Provides an effective 'instrumentation layering'. A listener with the higher priority will
   * replace any previously registered instance. Likewise, a listener with a lower priority will
   * never be registered over the one with higher priority.
   *
   * @return a relative priority number
   */
  public int getPriority() {
    return priority;
  }

  /**
   * A convenience method to register a listener instance and obeying the priority.
   *
   * @param listener the instance to register
   * @param listeners the existing collection of listeners
   * @return the registered instance or {@literal null}
   */
  public static MongoCommandListener tryRegister(
      MongoCommandListener listener, List<CommandListener> listeners) {
    if (listeners.isEmpty()) {
      listeners.add(listener);
      return listener;
    }

    ListIterator<CommandListener> iterator = listeners.listIterator();
    while (iterator.hasPrevious()) {
      CommandListener previous = iterator.previous();
      if (previous instanceof MongoCommandListener) {
        if (((MongoCommandListener) previous).getPriority() < listener.getPriority()) {
          iterator.remove();
          listeners.add(listener);
          return listener;
        }
      }
    }
    return null;
  }

  @Override
  public void commandStarted(final CommandStartedEvent event) {
    if (listenerAccessor != null) {
      listenerAccessor.putIfAbsent(event.getConnectionDescription(), this);
    }
    final AgentSpan span = startSpan(MongoDecorator.MONGO_QUERY);
    try (final AgentScope scope = activateSpan(span)) {
      decorator.afterStart(span);
      decorator.onConnection(span, event);
      // overlay Mongo application name if we have it (replaces the deprecated cluster description)
      if (applicationName != null) {
        span.setTag(Tags.DB_INSTANCE, applicationName);
        if (Config.get().isDbClientSplitByInstance()) {
          span.setServiceName(applicationName);
        }
      }
      if (event.getConnectionDescription() != null
          && event.getConnectionDescription() != null
          && event.getConnectionDescription().getServerAddress() != null) {
        // cannot use onPeerConnection because ServerAddress.getSocketAddress()
        // may do a DNS lookup
        ServerAddress serverAddress = event.getConnectionDescription().getServerAddress();
        span.setTag(Tags.PEER_HOSTNAME, serverAddress.getHost())
            .setTag(Tags.PEER_PORT, PORTS.get(serverAddress.getPort()))
            .setTag(
                Tags.DB_OPERATION,
                COMMAND_NAMES.computeIfAbsent(event.getCommandName(), UTF8_ENCODE));
      }
      decorator.onStatement(span, event.getCommand(), byteBufAccessor);
      spanMap.put(event.getRequestId(), new SpanEntry(span));
    }
  }

  @Override
  public void commandSucceeded(final CommandSucceededEvent event) {
    finishSpah(event.getRequestId(), null);
  }

  @Override
  public void commandFailed(final CommandFailedEvent event) {
    finishSpah(event.getRequestId(), event.getThrowable());
  }

  private void finishSpah(int requestId, Throwable t) {
    final SpanEntry entry = spanMap.remove(requestId);
    final AgentSpan span = entry != null ? entry.span : null;
    if (span != null) {
      if (t != null) {
        decorator.onError(span, t);
      }
      decorator.beforeFinish(span);
      if (entry.suspended) {
        // the span has been suspended but not resumed yet
        span.finishThreadMigration();
      }
      span.finish();
    }
  }

  public void suspendSpan(int requestId) {
    SpanEntry entry = spanMap.get(requestId);
    if (entry != null && entry.span != null) {
      entry.span.startThreadMigration();
      entry.suspended = true;
    }
  }
}
