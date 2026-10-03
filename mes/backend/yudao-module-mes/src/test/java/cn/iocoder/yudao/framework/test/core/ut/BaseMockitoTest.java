// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/framework/test/core/ut/BaseMockitoTest.java
package cn.iocoder.yudao.framework.test.core.ut;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 纯 Mockito 测试基类
 * 用于 Service 层单元测试，不启动 Spring Context，不连接数据库。
 * 速度极快，专注于验证逻辑链路。
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseMockitoTest {
}
