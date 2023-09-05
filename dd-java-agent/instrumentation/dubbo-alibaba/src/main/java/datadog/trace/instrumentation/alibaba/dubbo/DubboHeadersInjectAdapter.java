package datadog.trace.instrumentation.alibaba.dubbo;

import datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import com.alibaba.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DubboHeadersInjectAdapter implements AgentPropagation.Setter<RpcContext> {
  public static final DubboHeadersInjectAdapter SETTER = new DubboHeadersInjectAdapter();
  private static final Logger log = LoggerFactory.getLogger(DubboHeadersInjectAdapter.class);
  @Override
  public void set(RpcContext carrier, String key, String value) {
    if (log.isDebugEnabled()) {
//      System.out.println("dubbo Inject " + key + ":\t" + value);
      log.debug("dubbo Inject {} :\t {}" , key , value);
    }
    carrier.setAttachment(key, value);
//    carrier.getAttachments().put(key, value);
  }
}
