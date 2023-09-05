package datadog.trace.instrumentation.rocketmq5;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.rocketmq.client.apis.consumer.MessageListener;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.*;
@AutoService(Instrumenter.class)
public class ConsumeServiceInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForTypeHierarchy{
  public ConsumeServiceInstrumentation() {
    super("rocketmq-5.0");
  }

  @Override
  public String hierarchyMarkerType() {
    return ("org.apache.rocketmq.client.java.impl.consumer.ConsumeService");
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return named(hierarchyMarkerType());
  }
  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".ReceiveSpanFinishingCallback",
        packageName + ".MessageListenerWrapper",
        packageName + ".MessageMapSetter",
        packageName + ".MessageViewGetter",
        packageName + ".SendSpanFinishingCallback",
        packageName + ".Timer",
    };
  }
  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
  transformation.applyAdvice(
      isConstructor()
          .and(isPublic()
          .and(takesArgument(1, named("org.apache.rocketmq.client.apis.consumer.MessageListener")))),
      ConsumeServiceInstrumentation.class.getName() + "$ConstructorAdvice");
  }

  public static class ConstructorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(value = 1, readOnly = false) MessageListener messageListener) {
      // Replace messageListener by wrapper.
      if (!(messageListener instanceof MessageListenerWrapper)) {
        messageListener = new MessageListenerWrapper(messageListener);
      }
    }
  }
}
