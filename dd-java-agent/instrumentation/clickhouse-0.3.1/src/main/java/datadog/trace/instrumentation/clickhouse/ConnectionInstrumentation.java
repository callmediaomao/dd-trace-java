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

package datadog.trace.instrumentation.clickhouse;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;

import java.util.Collections;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class ConnectionInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  public ConnectionInstrumentation() {
    super("clickhouse");
  }

  private final static String ENHANCE_CLASS = "ru.yandex.clickhouse.ClickHouseConnectionImpl";
  private final static String INIT_CONNECTION_METHOD_NAME = "initConnection";
  private final static String CREATE_CLICKHOUSE_STATEMENT_METHOD_NAME = "createClickHouseStatement";

  public static final String CLOSE_METHOD_NAME = "close";

  public static final String PREPARE_STATEMENT_METHOD_NAME = "prepareStatement";

  private static final String CREATE_STATEMENT_METHOD_NAME = "createStatement";

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".CreateStatementAdvice",
        packageName + ".PrepareStatementAdvice",
        packageName + ".CloseAdvice",
        packageName + ".InitConnectionAdvice",
        packageName + ".ClickhouseDecorator",
        packageName + ".ClickHouseStatementTracingWrapper",
        packageName + ".ClickHouseStatementTracingWrapper$SupplierWithException",
        packageName + ".TracedClickHouseStatement",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return Collections.singletonMap("ru.yandex.clickhouse.ClickHouseConnectionImpl", DBInfo.class.getName());
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isMethod()
            .and(named(CREATE_CLICKHOUSE_STATEMENT_METHOD_NAME)
                .or(named(CREATE_STATEMENT_METHOD_NAME)))
        , packageName + ".CreateStatementAdvice"
    );

    transformation.applyAdvice(
        isMethod()
            .and(named(PREPARE_STATEMENT_METHOD_NAME))
            .and(takesArgument(0,String.class)),
        packageName + ".PrepareStatementAdvice"
    );

    transformation.applyAdvice(
        isMethod()
            .and(named(CLOSE_METHOD_NAME)),
        packageName + ".CloseAdvice"
    );

    transformation.applyAdvice(
        isMethod()
            .and(isPrivate())
            .and(named(INIT_CONNECTION_METHOD_NAME))
            .and(takesArgument(0,
                named("ru.yandex.clickhouse.settings.ClickHouseProperties"))),
        packageName + ".InitConnectionAdvice"
    );
  }
}
