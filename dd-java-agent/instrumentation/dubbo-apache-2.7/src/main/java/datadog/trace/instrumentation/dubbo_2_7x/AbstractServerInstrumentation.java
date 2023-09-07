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

import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class AbstractServerInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "org.apache.dubbo.remoting.transport.AbstractServer";

  public AbstractServerInstrumentation() {
    super("apache-dubbo");
  }


  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
      transformation.applyAdvice(
          isConstructor()
              .and(takesArguments(2))
              .and(takesArgument(0,named("org.apache.dubbo.common.URL"))),
          AbstractServerInstrumentation.class.getName()+"$AbstractServerAdvice"
      );
  }

  // TODO tag注入
  public static class AbstractServerAdvice {
  }
}
