package cn.iocoder.yudao.framework.test.core.annotations;

import java.lang.annotation.*;

/**
 * SDET 强制测试描述注解
 * 用于在日志中清晰打印测试意图
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TestScenario {

    /**
     * 测试场景描述 (Scenario)
     * 例如: "工单处于待生产状态，且库存充足"
     */
    String scenario();

    /**
     * 测试目标 (Goal)
     * 例如: "验证点击开工后，状态流转为DOING，且记录实际开始时间"
     */
    String goal();
}
