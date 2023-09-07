package datadog.trace.instrumentation.redisson3;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import net.bytebuddy.asm.Advice;
import org.redisson.config.Config;
import org.redisson.connection.ConnectionManager;

import java.util.Collection;
import java.util.Map;

import static datadog.trace.instrumentation.redisson3.Redisson3ClientDecorator.DECORATE;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

@AutoService(Instrumenter.class)
public class ConnectionManagerInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "org.redisson.connection.MasterSlaveConnectionManager";

  public ConnectionManagerInstrumentation() {
    super("redisson", "redis");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".Redisson3ClientDecorator",
        packageName + ".RedissonInfo"
    };
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(named("createClient")),
        ConnectionManagerInstrumentation.class.getName() + "$CreateClientAdvice"
    );
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("org.redisson.connection.ConnectionManager", RedissonInfo.class.getName());
  }

  public static class CreateClientAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void afterSpan(
        @Advice.Thrown final Throwable throwable,
        @Advice.This final ConnectionManager connectionManager) {
      ContextStore<ConnectionManager, RedissonInfo> contextStore = InstrumentationContext.get(ConnectionManager.class, RedissonInfo.class);
      //每次都覆盖
      RedissonInfo redissonInfo = contextStore.get(connectionManager);
      if (redissonInfo == null){
        redissonInfo = new RedissonInfo();
      }

      try {
        Config config = connectionManager.getCfg();
        Object singleServerConfig = DECORATE.getObjectField(config, "singleServerConfig");
        Object sentinelServersConfig = DECORATE.getObjectField(config, "sentinelServersConfig");
        Object masterSlaveServersConfig = DECORATE.getObjectField(config, "masterSlaveServersConfig");
        Object clusterServersConfig = DECORATE.getObjectField(config, "clusterServersConfig");
        Object replicatedServersConfig = DECORATE.getObjectField(config, "replicatedServersConfig");

        StringBuilder peer = new StringBuilder();

        if (singleServerConfig != null) {
          Object singleAddress = DECORATE.getObjectField(singleServerConfig, "address");
          peer.append(DECORATE.getPeer(singleAddress));
          redissonInfo.setRedissonAddress(DECORATE.shorten(peer.toString()));
          contextStore.put(connectionManager,redissonInfo);
          return;
        }
        if (sentinelServersConfig != null) {
          DECORATE.appendAddresses(peer, (Collection) DECORATE.getObjectField(sentinelServersConfig, "sentinelAddresses"));
          redissonInfo.setRedissonAddress(DECORATE.shorten(peer.toString()));
          contextStore.put(connectionManager,redissonInfo);
          return;
        }
        if (masterSlaveServersConfig != null) {
          Object masterAddress = DECORATE.getObjectField(masterSlaveServersConfig, "masterAddress");
          peer.append(DECORATE.getPeer(masterAddress));
          DECORATE.appendAddresses(peer, (Collection) DECORATE.getObjectField(masterSlaveServersConfig, "slaveAddresses"));
          redissonInfo.setRedissonAddress(DECORATE.shorten(peer.toString()));
          contextStore.put(connectionManager,redissonInfo);
          return;
        }
        if (clusterServersConfig != null) {
          DECORATE.appendAddresses(peer, (Collection) DECORATE.getObjectField(clusterServersConfig, "nodeAddresses"));
          redissonInfo.setRedissonAddress(DECORATE.shorten(peer.toString()));
          contextStore.put(connectionManager,redissonInfo);
          return;
        }
        if (replicatedServersConfig != null) {
          DECORATE.appendAddresses(peer, (Collection) DECORATE.getObjectField(replicatedServersConfig, "nodeAddresses"));
          redissonInfo.setRedissonAddress(DECORATE.shorten(peer.toString()));
          contextStore.put(connectionManager,redissonInfo);
        }
      } catch (Exception e) {
      }
    }
  }
}
