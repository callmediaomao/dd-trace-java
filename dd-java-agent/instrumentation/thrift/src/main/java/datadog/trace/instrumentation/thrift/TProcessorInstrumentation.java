package datadog.trace.instrumentation.thrift;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.implementsInterface;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static datadog.trace.instrumentation.thrift.ThriftConstants.*;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @Description
 * @Author lenovo
 * @Date 2022/11/24 9:34
 */
@AutoService(Instrumenter.class)
public class TProcessorInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForTypeHierarchy {

  public TProcessorInstrumentation() {
    super(INSTRUMENTATION_NAME, INSTRUMENTATION_NAME_SERVER);
  }

  @Override
  public String hierarchyMarkerType() {
    return T_PROCESSOR;
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return implementsInterface(namedOneOf(hierarchyMarkerType(),T_ASYNC_PROCESSOR))
        .and(not(named(T_BASE_ASYNC_PROCESSOR)))
        .and(not(named(T_BASE_PROCESSOR)))
        .and(not(named(T_MULTIPLEXED_PROCESSOR)));
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(isMethod()
            .and(isPublic())
            .and(named("process"))
        , packageName + ".TProcessorProcessAdvice");

  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".ThriftConstants",
        packageName + ".ThriftBaseDecorator",
        packageName + ".ThriftConstants$Tags",
        packageName + ".AbstractContext",
        packageName + ".ServerInProtocolWrapper",
        packageName + ".ExtractAdepter",
        packageName + ".CTProtocolFactory",
        packageName + ".STProtocolFactory",
        packageName + ".ThriftServerDecorator",
        packageName + ".TProcessorProcessAdvice",
        packageName + ".Context"
    };
  }
}
