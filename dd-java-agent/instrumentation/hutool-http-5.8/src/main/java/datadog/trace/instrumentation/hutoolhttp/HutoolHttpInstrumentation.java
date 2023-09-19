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

package datadog.trace.instrumentation.hutoolhttp;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.hutoolhttp.HutoolHttpClientDecorator.DECORATE;
import static net.bytebuddy.matcher.ElementMatchers.*;

// hutool-http请求能够被捕获到
//@AutoService(Instrumenter.class)
public class HutoolHttpInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "cn.hutool.http.HttpRequest";

  public HutoolHttpInstrumentation() {
    super("hutool-http");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName+".HutoolHttpClientDecorator",
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
              .and(named("execute"))
              .and(takesArguments(1)),
          HutoolHttpInstrumentation.class.getName()+"$ExecuteAdvice"
      );
  }

  public static class ExecuteAdvice{
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope start(
        @Advice.This final HttpRequest httpRequest
    ){
      AgentSpan agentSpan = startSpan("hutool-http");
      DECORATE.afterStart(agentSpan);
      DECORATE.onRequest(agentSpan,httpRequest);
      return activateSpan(agentSpan);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exit(@Advice.Enter final AgentScope scope,
                            @Advice.Return final HttpResponse httpResponse,
                            @Advice.Thrown final Throwable throwable) {
      if (scope == null){
        return;
      }
      DECORATE.onResponse(scope.span(),httpResponse);
      DECORATE.onError(scope, throwable);
      DECORATE.beforeFinish(scope);
      scope.close();
      scope.span().finish();
    }
  }
}
