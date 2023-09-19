package datadog.trace.instrumentation.mongo2;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.ServerAddress;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import net.bytebuddy.asm.Advice;

import java.util.List;

public class ConstructorAdvice {
  @Advice.OnMethodExit
  public static void afterConstruct(
      @Advice.Argument(0) final DB db,
      @Advice.This final DBCollection dbCollection
  ){
    List<ServerAddress> servers = db.getMongo().getAllAddress();
    StringBuilder peers = new StringBuilder();
    for (ServerAddress address : servers) {
      peers.append(address.getHost()).append(":").append(address.getPort()).append(";");
    }
    ContextStore<DBCollection, String> contextStore = InstrumentationContext.get(DBCollection.class, String.class);
    contextStore.put(dbCollection,peers.subSequence(0, peers.length() - 1).toString());
  }
}
