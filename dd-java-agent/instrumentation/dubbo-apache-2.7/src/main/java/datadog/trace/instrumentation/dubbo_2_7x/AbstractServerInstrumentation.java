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
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import net.bytebuddy.asm.Advice;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.transport.AbstractServer;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class AbstractServerInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "org.apache.dubbo.remoting.transport.AbstractServer";

  public AbstractServerInstrumentation() {
    super("apache-dubbo");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName+".DubboInfo",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("org.apache.dubbo.remoting.transport.AbstractServer",DubboInfo.class.getName());
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

  public static class AbstractServerAdvice {
    @Advice.OnMethodExit
    public static void after(@Advice.Argument(0) final URL url,
                             @Advice.This final AbstractServer objInst) {
      try {
        Field executorField = AbstractServer.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        //通过反射去获取
        ExecutorService executor = (ExecutorService) executorField.get(objInst);
        int port = url.getPort();

        if (!(executor instanceof ThreadPoolExecutor)) {
          return;
        }
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        String threadPoolName = String.format("DubboServerHandler-%s", port);
        //覆盖掉原有的
        ContextStore<AbstractServer, DubboInfo> contextStore = InstrumentationContext.get(AbstractServer.class, DubboInfo.class);
        //每次都覆盖
        DubboInfo dubboInfo = contextStore.get(objInst);
        if (dubboInfo == null){
          dubboInfo = new DubboInfo();
        }
        dubboInfo.setThreadPoolName(threadPoolName);
        dubboInfo.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        dubboInfo.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        dubboInfo.setLargestPoolSize(threadPoolExecutor.getLargestPoolSize());
        dubboInfo.setPoolSize(threadPoolExecutor.getPoolSize());
        dubboInfo.setQueueSize(threadPoolExecutor.getQueue().size());
        dubboInfo.setActiveCount(threadPoolExecutor.getActiveCount());
        dubboInfo.setTaskCount(threadPoolExecutor.getTaskCount());
        dubboInfo.setCompletedTaskCount(threadPoolExecutor.getCompletedTaskCount());
        contextStore.put(objInst,dubboInfo);
      } catch (Exception e) {
      }
    }
  }
}
