package datadog.trace.instrumentation.rocketmq;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.UTF8BytesString;
import datadog.trace.bootstrap.instrumentation.decorator.BaseDecorator;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.SocketAddress;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.*;
import static datadog.trace.instrumentation.rocketmq.TextMapExtractAdapter.GETTER;
import static datadog.trace.instrumentation.rocketmq.TextMapInjectAdapter.SETTER;

public class RocketMqDecorator extends BaseDecorator {
  private static final Logger log = LoggerFactory.getLogger(RocketMqDecorator.class);
  public static final CharSequence ROCKETMQ_NAME = UTF8BytesString.create("rocketmq");
  private static final String BROKER_HOST = "bornHost";
  private static final String BROKER_ADDR = "bornAddr";
  private static final String BROKER_NAME = "brokerName";
  private static final String TOPIC = "topic";
  private static final String MESSAGING_ROCKETMQ_TAGS = "messaging.rocketmq.tags";
  private static final String MESSAGING_ROCKETMQ_BROKER_ADDRESS = "messaging.rocketmq.broker_address";
  private static final String MESSAGING_ROCKETMQ_SEND_RESULT = "messaging.rocketmq.send_result";
  private static final String MESSAGING_ROCKETMQ_QUEUE_ID = "messaging.rocketmq.queue_id";
  private static final String MESSAGING_ID = "messaging.id";
  private static final String MESSAGING_ROCKETMQ_QUEUE_OFFSET = "messaging.rocketmq.queue_offset";

  RocketMqDecorator() {
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[]{"rocketmq", "rocketmq-client"};
  }

  @Override
  protected CharSequence spanType() {
    return ROCKETMQ_NAME;
  }

  @Override
  protected CharSequence component() {
    return null;
  }

  private static final String LOCAL_SERVICE_NAME = "rocketmq";


  public AgentScope start(ConsumeMessageContext context) {
    MessageExt ext = context.getMsgList().get(0);
    AgentSpan.Context parentContext = propagate().extract(ext, GETTER);
    UTF8BytesString name = UTF8BytesString.create(ext.getTopic() + " receive");
    final AgentSpan span = startSpan(name, parentContext);
    span.setResourceName(name);

    span.setServiceName(LOCAL_SERVICE_NAME);

    span.setTag(BROKER_NAME, ext.getBrokerName());
    String tags = ext.getTags();
    if (tags != null) {
      span.setTag(MESSAGING_ROCKETMQ_TAGS, tags);
    }
    span.setTag(TOPIC, ext.getTopic());
    span.setTag(MESSAGING_ROCKETMQ_QUEUE_ID, ext.getQueueId());
    span.setTag(MESSAGING_ROCKETMQ_QUEUE_OFFSET, ext.getQueueOffset());
    span.setTag(MESSAGING_ID, ext.getMsgId());
    SocketAddress storeHost = ext.getStoreHost();
    if (storeHost != null) {
      span.setTag(MESSAGING_ROCKETMQ_BROKER_ADDRESS, getBrokerHost(storeHost));
    }
    afterStart(span);
    AgentScope scope = activateSpan(span);
    if (log.isDebugEnabled()){
      log.debug("consumer span start topic:{}",ext.getTopic());
    }
    return scope;
  }

  private static String getBrokerHost(SocketAddress storeHost) {
    return storeHost.toString().replace("/", "");
  }

  public void end(ConsumeMessageContext context) {
    String status = context.getStatus();
    AgentScope scope = activeScope();
    scope.span().setTag("status", status);
    beforeFinish(scope);
    scope.span().finish();
    scope.close();
    if (log.isDebugEnabled()){
      log.debug("consumer span end");
    }
  }

  public AgentScope start(SendMessageContext context) {
    String topic = context.getMessage().getTopic();
    UTF8BytesString spanName = UTF8BytesString.create(topic + " send");
    final AgentSpan span = startSpan(spanName);
    span.setResourceName(spanName);

    span.setTag(BROKER_HOST, context.getBornHost());
    span.setTag(BROKER_ADDR, context.getBrokerAddr());
    span.setServiceName(LOCAL_SERVICE_NAME);
    if (context.getMessage() != null) {
      String tags = context.getMessage().getTags();
      if (tags != null) {
        span.setTag(MESSAGING_ROCKETMQ_TAGS, tags);
      }
    }

    Message message = context.getMessage();
    if (null != message) {
      span.setTag(TOPIC, message.getTopic());
    }
    SendResult sendResult = context.getSendResult();
    if (null != sendResult) {
      span.setTag(MESSAGING_ID, sendResult.getMsgId());
    }
    String brokerAddr = context.getBrokerAddr();
    if (brokerAddr != null) {
      span.setTag(MESSAGING_ROCKETMQ_BROKER_ADDRESS, brokerAddr);
    }

    afterStart(span);
    propagate().inject(span, context, SETTER);
    AgentScope scope = activateSpan(span);
    if (log.isDebugEnabled()){
      log.debug("consumer span start topic:{}",topic);
    }
    return scope;
  }

  public void end(SendMessageContext context) {
    Exception exception = context.getException();
    AgentScope scope = activeScope();
    if (null != exception) {
      onError(scope, exception);
    }
    if (context.getSendResult() != null&&context.getSendResult().getSendStatus() != null){
      scope.span().setTag(MESSAGING_ROCKETMQ_SEND_RESULT, context.getSendResult().getSendStatus().name());
    }

    beforeFinish(scope);
    scope.span().finish();
    scope.close();
    if (log.isDebugEnabled()){
      log.debug("consumer span end");
    }
  }
}

