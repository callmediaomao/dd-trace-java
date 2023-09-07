/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package datadog.trace.instrumentation.dubbo_2_7x;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;

import java.util.Collections;

import static datadog.trace.instrumentation.dubbo_2_7x.DubboDecorator.DECORATE;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class DubboInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

    private static final String ENHANCE_CLASS = "org.apache.dubbo.monitor.support.MonitorFilter";

  public DubboInstrumentation() {
    super("apache-dubbo");
  }


  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(named("invoke"))
            .and(takesArguments(2)
                .and(takesArgument(0,named("org.apache.dubbo.rpc.Invoker")))
                .and(takesArgument(1,named("org.apache.dubbo.rpc.Invocation")))),
        DubboInstrumentation.class.getName()+"$DubboAdvice"

    );
  }

  public static class DubboAdvice {


    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope beginRequest(@Advice.Argument(1) final Invocation invocation,
                                          @Advice.Argument(0) final Invoker invoker
                                          ) {
      AgentScope scope = DECORATE.buildSpan(invoker, invocation);
      return scope;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Enter final AgentScope scope, @Advice.Thrown final Throwable throwable) {
      if (scope == null) {
        return;
      }
      DECORATE.onError(scope.span(), throwable);
      DECORATE.beforeFinish(scope.span());

      scope.close();
      scope.span().finish();
    }
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".DubboDecorator",
        packageName + ".DubboTraceInfo",
        packageName + ".HostAndPort",
        packageName + ".DubboConstants",
        packageName + ".DubboHeadersExtractAdapter",
        packageName + ".DubboHeadersInjectAdapter"
    };
  }
}
