package datadog.trace.instrumentation.redisson3;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import net.bytebuddy.asm.Advice;

@AutoService(Instrumenter.class)
public class RedissonClientInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "org.redisson.client.RedisClient";

  public RedissonClientInstrumentation() {
    super("redisson", "redis");
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    // todo isConstructor不能捕获构造器
//    transformation.applyAdvice(
//        isConstructor()
//        ,RedissonClientInstrumentation.class.getName()+"$RedisClientAdvice"
//    );
  }

  // todo skywalking is empty
  public static class RedisClientAdvice {
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan( @Advice.Thrown final Throwable throwable) {

    }
  }
}
