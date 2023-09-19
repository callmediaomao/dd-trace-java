package datadog.trace.instrumentation.datasource;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.jdbc.DBInfo;

import java.util.HashMap;
import java.util.Map;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.*;


/**
 * @Author: FanGang
 * @Date: 2023/8/30 14:41
 */
@AutoService(Instrumenter.class)
public class DruidAddDataSourceInstrumentation extends Instrumenter.Tracing
    implements Instrumenter.ForSingleType {

  public DruidAddDataSourceInstrumentation() {
    super("druidDatasource","datasource");
  }

  public String instrumentedType() {
    return "com.alibaba.druid.stat.DruidDataSourceStatManager";
  }

  public String[] helperClassNames() {
    return new String[]{
        packageName + ".DataSourceDecorator",
        packageName + ".DruidDataSourceDecorator",
        packageName + ".DataSourceAdvice",
//        packageName + ".DruidDataSourceInfo",
//        packageName + ".DruidDataSourceInfo$Builder",
        packageName + ".DataSourceConstant",
    };
  }

  public Map<String, String> contextStore() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("com.alibaba.druid.pool.DruidDataSource", DruidDataSourceInfo.class.getName());
    map.put("java.sql.Connection", DBInfo.class.getName());
    return map;
  }

  public void adviceTransformations(AdviceTransformation transformation) {
    // 静态方法
    transformation.applyAdvice(
        isMethod()
            .and(isStatic())
            .and(named("addDataSource"))
            .and(takesArguments(2).and(takesArgument(0, Object.class))),

        packageName + ".DataSourceAdvice"
    );
  }

}
