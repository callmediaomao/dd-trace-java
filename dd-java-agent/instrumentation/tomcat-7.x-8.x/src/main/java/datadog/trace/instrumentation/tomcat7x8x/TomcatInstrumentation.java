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

package datadog.trace.instrumentation.tomcat7x8x;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.tomcat7x8x.TomcatDecorator.DECORATE;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class TomcatInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  /**
   * Enhance class.
   */
  private static final String ENHANCE_CLASS = "org.apache.catalina.core.StandardHostValve";

  public TomcatInstrumentation() {
    super("tomcat");
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  //将skywalking方法复制过来直接进行适配
  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
      transformation.applyAdvice(
          isMethod()
              .and(named("invoke"))
              .and(takesArguments(2)
                  .and(takesArgument(0,named("org.apache.catalina.connector.Request")))
                  .and(takesArgument(1,named("org.apache.catalina.connector.Response")))),
          TomcatInstrumentation.class.getName()+"$InvokeAdvice"
      );

    transformation.applyAdvice(
        isMethod()
            .and(named("throwable"))
            .and(takesArguments(3)
                .and(takesArgument(0,named("org.apache.catalina.connector.Request")))
                .and(takesArgument(1,named("org.apache.catalina.connector.Response")))
                .and(takesArgument(2,Throwable.class))),
        TomcatInstrumentation.class.getName()+"$ThrowableAdvice"
    );
  }

  public static class InvokeAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope beforeMeth(@Advice.Argument(0) final ServletRequest req,
                                        @Advice.Argument(1) final ServletResponse resp,
                                        @Advice.This final RequestDispatcher applicationDispatcher) {
      AgentSpan span = startSpan("tomcat");
      DECORATE.afterStart(span);
      AgentScope agentScope = activateSpan(span);
      return agentScope;

    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void afterMeho(@Advice.Enter final AgentScope scope,
                                 @Advice.Thrown final Throwable throwable) {
      if (scope == null){
        return;
      }
      DECORATE.beforeFinish(scope);
      DECORATE.onError(scope,throwable);
      scope.span().finish();
      scope.close();
    }

  }

}
