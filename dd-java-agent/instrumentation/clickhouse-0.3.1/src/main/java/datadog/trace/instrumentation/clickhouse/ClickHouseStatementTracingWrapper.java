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

import datadog.trace.bootstrap.CallDepthThreadLocalMap;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;

import java.sql.SQLException;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.clickhouse.ClickhouseDecorator.DECORATE;

/**
 *
 */
public class ClickHouseStatementTracingWrapper {

  public static final String CREATE_STATEMENT = "clickhouse.statement";

  public static <T> T of(DBInfo dbInfo, String methodName, String sql,
                         SupplierWithException<T> supplier) throws SQLException {
    AgentScope agentScope = null;
    try {
      int depth = CallDepthThreadLocalMap.incrementCallDepth(ClickHouseStatementTracingWrapper.class);
      if(depth > 0){
        return supplier.get();
      }
      final AgentSpan span = startSpan(CREATE_STATEMENT);
      DECORATE.afterStart(span);
      DECORATE.onConnection(
          span, dbInfo);
      DECORATE.onStatement(span, sql);
      agentScope = activateSpan(span);
      return supplier.get();
    } catch (Exception e) {

    } finally {
      if (agentScope != null) {
        DECORATE.beforeFinish(agentScope.span());
        agentScope.close();
        agentScope.span().finish();
        CallDepthThreadLocalMap.reset(ClickHouseStatementTracingWrapper.class);
      }
    }
    return null;
  }

  public interface SupplierWithException<T> {

    T get() throws SQLException;
  }

}
