package cn.iocoder.yudao.server.framework.local;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.DispatcherType;

/**
 * Fail-closed guards for the isolated DoorMES copy only. No production profile is affected.
 *
 * This is deliberately not a general network sandbox: it protects Spring RestTemplate,
 * system SMS/mail services and the infrastructure file-client factory. Other transports,
 * report JDBC data sources and URLs rendered by the browser need separate isolation.
 */
@Configuration(proxyBeanMethods = false)
@Profile("doormes")
public class DoormesLocalSafetyConfiguration {

    static final String DEFAULT_UPLOAD_ROOT =
            "E:/doorMES_v1/LUCK-MES-HC-0/runtime-local/uploads";

    /** Static to avoid prematurely initializing application services during BPP registration. */
    @Bean
    public static BeanPostProcessor doormesLocalSafetyBeanPostProcessor(Environment environment) {
        String uploadRoot = environment.getProperty("doormes.local.upload-root", DEFAULT_UPLOAD_ROOT);
        String fileOrigin = environment.getProperty("doormes.local.file-origin",
                "http://127.0.0.1:" + environment.getProperty("server.port", "48082"));
        return new LocalSafetyBeanPostProcessor(new DoormesLocalFileSafety(uploadRoot, fileOrigin));
    }

    @Bean
    public LocalOutboundAspect doormesLocalOutboundAspect() {
        return new LocalOutboundAspect();
    }

    @Bean
    public FilterRegistrationBean<DoormesLocalRestrictedRouteFilter> doormesLocalRestrictedRouteFilter() {
        FilterRegistrationBean<DoormesLocalRestrictedRouteFilter> registration =
                new FilterRegistrationBean<>(new DoormesLocalRestrictedRouteFilter());
        registration.setName("doormesLocalRestrictedRoutes");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD,
                DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    static final class LocalSafetyBeanPostProcessor implements BeanPostProcessor, Ordered {

        private final DoormesLocalFileSafety fileSafety;

        LocalSafetyBeanPostProcessor(DoormesLocalFileSafety fileSafety) {
            this.fileSafety = fileSafety;
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof RestTemplate restTemplate) {
                List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
                // First interceptor: reject before another interceptor or the transport can run.
                interceptors.add(new NoOutboundHttpInterceptor());
                interceptors.addAll(restTemplate.getInterceptors());
                restTemplate.setInterceptors(interceptors);
            }
            if (bean instanceof FileClientFactory factory) {
                return fileSafety.guardFactory(factory);
            }
            return bean;
        }
    }

    private static final class NoOutboundHttpInterceptor implements ClientHttpRequestInterceptor, PriorityOrdered {

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
            // Not a RestClientException: the BPM helper must not log headers/body on denial.
            throw new IllegalStateException("doormes 本地安全模式禁止 RestTemplate HTTP 外发");
        }
    }

    @Aspect
    public static class LocalOutboundAspect {

        /** Guard both the user-facing send methods and the asynchronous consumer entry points. */
        @Around("execution(* cn.iocoder.yudao.module.system.service.sms.SmsSendService+.send*(..))"
                + " || execution(* cn.iocoder.yudao.module.system.service.sms.SmsSendService+.doSend*(..))"
                + " || execution(* cn.iocoder.yudao.module.system.service.mail.MailSendService+.send*(..))"
                + " || execution(* cn.iocoder.yudao.module.system.service.mail.MailSendService+.doSend*(..))")
        public Object blockMessageSending(ProceedingJoinPoint ignored) {
            throw new IllegalStateException("doormes 本地安全模式禁止短信和邮件外发");
        }
    }
}
