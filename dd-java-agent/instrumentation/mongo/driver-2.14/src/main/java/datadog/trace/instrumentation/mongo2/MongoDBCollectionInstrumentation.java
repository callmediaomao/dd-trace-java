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

/**
 * {@link MongoDBCollectionInstrumentation} define that the MongoDB Java Driver 2.13.x-2.14.x plugin intercepts the
 * following methods in the {@link com.mongodb.DBCollection}class: 1. aggregate 2. findAndModify 3. getCount
 * <p>
 * 4. drop 5. dropIndexes 6. rename 7. group 8. distinct 9. mapReduce
 */

@AutoService(Instrumenter.class)
public class MongoDBCollectionInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  private static final String ENHANCE_CLASS = "com.mongodb.DBCollection";

  public MongoDBCollectionInstrumentation() {
    super("mongodb-collection");
  }

  @Override
  public String[] helperClassNames() {
    return new String[]{
        packageName + ".ConstructorAdvice",
        packageName + ".MethodAdvice",
        packageName + ".MongoDecorator",
        packageName + ".ReVoidMethodAdvice",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("com.mongodb.DBCollection", String.class.getName());
  }

  @Override
  public String instrumentedType() {
    return ENHANCE_CLASS;
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isConstructor(),
        packageName + ".ConstructorAdvice"
    );

    transformation.applyAdvice(isMethod().and(named("aggregate"))
        .and(takesArgument(1, named("com.mongodb.ReadPreference"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("findAndModify"))
        .and(takesArguments(9)), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("getCount"))
        .and(takesArgument(6, named("java.util.concurrent.TimeUnit"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("drop")), packageName + ".ReVoidMethodAdvice");
    transformation.applyAdvice(isMethod().and(named("explainAggregate")), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("dropIndexes")), packageName + ".ReVoidMethodAdvice");
    transformation.applyAdvice(isMethod().and(named("rename"))
        .and(takesArgument(1, named("boolean"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("group"))
        .and(takesArgument(1, named("boolean"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("group"))
        .and(takesArgument(1, named("com.mongodb.DBObject"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("distinct"))
        .and(takesArgument(2, named("com.mongodb.ReadPreference"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("mapReduce"))
        .and(takesArgument(0, named("com.mongodb.MapReduceCommand"))), packageName + ".MethodAdvice");
    transformation.applyAdvice(isMethod().and(named("mapReduce"))
        .and(takesArgument(0, named("com.mongodb.DBObject"))), packageName + ".MethodAdvice");

  }
}
