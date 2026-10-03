// 文件路径: backend/yudao-module-mes/src/test/java/cn/iocoder/yudao/framework/test/core/ut/BaseManualTest.java

package cn.iocoder.yudao.framework.test.core.ut;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.annotations.TestScenario;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Optional;

@Import(BaseManualTest.ManualTestInfrastructureConfig.class)
public abstract class BaseManualTest extends BaseDbUnitTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    protected JdbcTemplate jdbcTemplate; // 🚀 修改为 protected，方便子类使用

    protected TestTraceRegistry tracer;

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        this.tracer = new TestTraceRegistry(jdbcTemplate);

        // 1. 构造模拟用户
        LoginUser loginUser = new LoginUser();
        loginUser.setId(1L);
        loginUser.setTenantId(1L);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setScopes(new ArrayList<>());

        // 2. 构造 Mock Request
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();

        // 3. 设置上下文
        try {
            SecurityFrameworkUtils.setLoginUser(loginUser, mockRequest);
        } catch (Exception e) {
            log.warn("⚠️ [BaseManualTest] 设置 SecurityContext 警告: {}", e.getMessage());
        }
        TenantContextHolder.setTenantId(1L);

        // 日志美化
        Optional<TestScenario> scenarioOpt = Optional.ofNullable(testInfo.getTestMethod().get().getAnnotation(TestScenario.class));
        System.out.println("\n🟦🟦🟦 [开始测试] " + testInfo.getDisplayName() + " 🟦🟦🟦");
        if (scenarioOpt.isPresent()) {
            TestScenario ts = scenarioOpt.get();
            System.out.println("📝 测试场景: " + ts.scenario());
            System.out.println("🎯 测试目标: " + ts.goal());
        }
        System.out.println("--------------------------------------------------------");
    }

    @AfterEach
    public void tearDown() {
        // 🚀 核心修复 1: 优先执行 DB 清理！防止后续代码报错导致数据残留
        if (tracer != null) {
            try {
                tracer.executeWipe();
            } catch (Exception e) {
                log.error("❌ [TraceRegistry] 自动清理执行失败", e);
            }
        }

        // 🚀 核心修复 2: 安全清理上下文，忽略 NPE
        TenantContextHolder.clear();
        try {
            // 尝试传入 null 清理，如果框架报错则捕获
            SecurityFrameworkUtils.setLoginUser(null, new MockHttpServletRequest());
        } catch (Exception e) {
            log.warn("⚠️ [BaseManualTest] 清理 SecurityContext 失败 (已忽略): {}", e.getMessage());
        }
    }

    protected void track(String tableName, Long id) {
        if (tracer != null) {
            tracer.track(tableName, id);
        }
    }

    @TestConfiguration
    @EnableConfigurationProperties
    public static class ManualTestInfrastructureConfig {
        @Bean
        @Primary
        @ConfigurationProperties(prefix = "spring.datasource")
        public DataSourceProperties dataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        @Primary
        public DataSource dataSource(DataSourceProperties properties) {
            return properties.initializeDataSourceBuilder()
                    .type(HikariDataSource.class)
                    .build();
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
