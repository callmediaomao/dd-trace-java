package datadog.trace.instrumentation.hsf;

import com.taobao.hsf.context.RPCContext;
import datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HSFInjectAdapter implements AgentPropagation.Setter<RPCContext> {
  public static final HSFInjectAdapter SETTER = new HSFInjectAdapter();
  private static final Logger log = LoggerFactory.getLogger(HSFInjectAdapter.class);
  @Override
  public void set(RPCContext carrier, String key, String value) {
    log.debug("hsf Inject " + key + ":\t" + value);
    carrier.putAttachment(key, value);
//    carrier.getAttachments().put(key, value);
  }
}
