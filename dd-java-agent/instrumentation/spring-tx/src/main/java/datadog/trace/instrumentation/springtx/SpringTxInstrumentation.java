/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package datadog.trace.instrumentation.springtx;


import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

@AutoService(Instrumenter.class)
public class SpringTxInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final Logger log = LoggerFactory.getLogger(SpringTxInstrumentation.class);

  public static final String CLASS_NAME = "org.springframework.transaction.support.AbstractPlatformTransactionManager";

  public SpringTxInstrumentation() {
    super("spring-tx");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".SpringTxDecorator",
        packageName + ".GetTransactionAdvice",
        packageName + ".CommintOrRollbackAdvice"
    };
  }

  @Override
  public String instrumentedType() {
    return CLASS_NAME;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    log.debug("-------进入方法！！！--------");
    transformation.applyAdvice(
        isMethod()
            .and(named("getTransaction"))
            .and(takesArgument(0,named("org.springframework.transaction.TransactionDefinition"))),
        packageName + ".GetTransactionAdvice"
    );

    transformation.applyAdvice(
        isMethod()
            .and(named("commit").or(named("rollback")))
            .and(takesArgument(0,named("org.springframework.transaction.TransactionStatus"))),
        packageName + ".CommintOrRollbackAdvice"
    );
    log.debug("--------方法退出！！！-------");
  }
}
