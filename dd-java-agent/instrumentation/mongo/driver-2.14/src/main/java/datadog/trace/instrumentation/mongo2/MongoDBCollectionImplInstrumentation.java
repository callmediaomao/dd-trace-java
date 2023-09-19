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

package datadog.trace.instrumentation.mongo2;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.*;

@AutoService(Instrumenter.class)
public class MongoDBCollectionImplInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "com.mongodb.DBCollectionImpl";

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName+".ConstructorAdvice",
        packageName+".MethodAdvice",
        packageName+".ReVoidMethodAdvice",
        packageName+".MongoDecorator",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("com.mongodb.DBCollection",String.class.getName());
  }

  public MongoDBCollectionImplInstrumentation() {
    super("mongodb-collectionImpl");
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    // 此处默认所有的remotepeers拿的是dbcollection的
    transformation.applyAdvice(
        isConstructor(),
        packageName+".ConstructorAdvice"
    );
    transformation.applyAdvice(isMethod().and(named("find")).and(takesArguments(9)),packageName+".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("insert")).and(takesArguments(4)),packageName+".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("insertImpl")),packageName+".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("update")),packageName+".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("updateImpl")),packageName+".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("remove")).and(takesArguments(4)),packageName+".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("createIndex")),packageName+".ReVoidMethodAdvice");
  }
}
