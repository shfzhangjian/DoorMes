package cn.iocoder.yudao.module.mes.framework.swagger.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MES 模块的 Swagger 配置
 * 用于在 Knife4j 接口文档中建立独立的 "MES 模块" 分组
 */
@Configuration(proxyBeanMethods = false)
public class MesSwaggerConfiguration {

    @Bean
    public GroupedOpenApi mesGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("MES", "/mes/**");
    }

}
