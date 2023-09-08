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
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Map;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.tomcat7x8x.TomcatDecorator.DECORATE;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class ApplicationDispatcherInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "org.apache.catalina.core.ApplicationDispatcher";
  private static final String ENHANCE_METHOD = "forward";

  public ApplicationDispatcherInstrumentation() {
    super("tomcat");
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".ExtractAdapter",
        packageName + ".ExtractAdapter$Request",
        packageName + ".ExtractAdapter$Response",
        packageName + ".TomcatDecorator",
        packageName + ".TomcatDecorator$TomcatBlockResponseFunction",
        packageName + ".TomcatBlockingHelper",
        packageName + ".RequestURIDataAdapter",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("javax.servlet.RequestDispatcher", String.class.getName());
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(named(ENHANCE_METHOD))
            .and(takesArguments(2).and(takesArgument(0, named("javax.servlet.ServletRequest")))
                .and(takesArgument(1, named("javax.servlet.ServletResponse")))),
        ApplicationDispatcherInstrumentation.class.getName() + "$ForwardAdvice"
    );

    transformation.applyAdvice(
        isConstructor()
            .and(takesArgument(1, String.class)),
        ApplicationDispatcherInstrumentation.class.getName() + "$ContructorAdvice"
    );
  }

  // todo 创建一个新的span用来记录就行了，用构造器把构造参数保存下来
  public static class ForwardAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope beforeMeth(@Advice.Argument(0) final ServletRequest req,
                                        @Advice.Argument(1) final ServletResponse resp,
                                        @Advice.This final RequestDispatcher applicationDispatcher) {
      // 1.开启一个线程
      // 2.通过http头部来判断
      ContextStore<RequestDispatcher, String> contextStore = InstrumentationContext.get(RequestDispatcher.class, String.class);
      String url = contextStore.get(applicationDispatcher);
      AgentSpan span = startSpan("tomcat");
      DECORATE.afterStart(span);
      span.setTag("forward-url",url == null ? "":url);
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

  public static class ContructorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void beforeMeth(@Advice.Argument(1) final String requestUrl,
                                  @Advice.This final RequestDispatcher applicationDispatcher) {
      ContextStore<RequestDispatcher, String> contextStore = InstrumentationContext.get(RequestDispatcher.class, String.class);
      String url = contextStore.get(applicationDispatcher);
      if (url == null) {
        url = requestUrl;
      }
      contextStore.put(applicationDispatcher, url);
    }
  }
}
