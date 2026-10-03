package cn.iocoder.yudao.server.framework.local;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmHttpRequestUtils;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactory;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactoryImpl;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClientConfig;
import cn.iocoder.yudao.module.system.mq.message.mail.MailSendMessage;
import cn.iocoder.yudao.module.system.mq.message.sms.SmsSendMessage;
import cn.iocoder.yudao.module.system.service.mail.MailSendService;
import cn.iocoder.yudao.module.system.service.mail.MailSendServiceImpl;
import cn.iocoder.yudao.module.system.service.sms.SmsSendService;
import cn.iocoder.yudao.module.system.service.sms.SmsSendServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Uses only test-owned temporary files and an in-memory context: no DB, Redis or live service. */
class DoormesLocalSafetyConfigurationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void productionProfileDoesNotInstallLocalGuards() {
        try (AnnotationConfigApplicationContext context = context("prod")) {
            assertEquals(0, context.getBean(RestTemplate.class).getInterceptors().size());
            assertInstanceOf(FileClientFactoryImpl.class, context.getBean(FileClientFactory.class));
            assertEquals(0, context.getBeansOfType(
                    DoormesLocalSafetyConfiguration.LocalOutboundAspect.class).size());
            assertEquals(0, context.getBeansOfType(FilterRegistrationBean.class).size());
        }
    }

    @Test
    void historicalPluginAndDatasourceRoutesAreDeniedBeforeTheirHandlers() throws Exception {
        DoormesLocalRestrictedRouteFilter filter = new DoormesLocalRestrictedRouteFilter();
        for (String path : new String[]{"/jmreport/view/old", "/jimubi/index", "/drag/onlDragDataSource/test",
                "/admin-api/jmreport/open", "/admin-api/infra/data-source-config/create",
                "/admin-api/infra/codegen/db/table/list", "/%6amreport/view/old",
                "/admin-api/mes/../infra/codegen/preview", "/jmreport;old/view"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicInteger handlerCalls = new AtomicInteger();
            filter.doFilter(request, response, (req, res) -> handlerCalls.incrementAndGet());
            assertEquals(403, response.getStatus(), path);
            assertEquals(0, handlerCalls.get(), path);
            assertTrue(response.getContentAsString().contains("\"code\":403"));
        }
    }

    @Test
    void loginMenusMesAndNewAttachmentRoutesRemainAvailable() throws Exception {
        DoormesLocalRestrictedRouteFilter filter = new DoormesLocalRestrictedRouteFilter();
        for (String path : new String[]{"/admin-api/system/auth/login", "/admin-api/system/auth/get-permission-info",
                "/admin-api/mes/order/list", "/admin-api/doormes/drawing/list",
                "/admin-api/infra/file/upload", "/admin-api/infra/file/101/get/example.txt", "/dragons"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicInteger handlerCalls = new AtomicInteger();
            filter.doFilter(request, response, (req, res) -> handlerCalls.incrementAndGet());
            assertEquals(1, handlerCalls.get(), path);
            assertEquals(200, response.getStatus(), path);
        }
    }

    @Test
    void doormesBlocksHttpIncludingTheHistoricalBpmHelperBeforeTransport() {
        try (AnnotationConfigApplicationContext context = context("doormes")) {
            RestTemplate restTemplate = context.getBean(RestTemplate.class);
            AtomicInteger transportCalls = new AtomicInteger();
            restTemplate.setRequestFactory((uri, method) -> {
                transportCalls.incrementAndGet();
                throw new AssertionError("A network transport must not be created");
            });
            assertThrows(IllegalStateException.class, () -> restTemplate.getForObject(
                    "https://invalid.example.test/request", String.class));
            assertThrows(IllegalStateException.class, () -> BpmHttpRequestUtils.sendHttpRequest(
                    "https://invalid.example.test/callback", new HttpHeaders(), Map.of(), restTemplate));
            assertEquals(0, transportCalls.get());
        }
    }

    @Test
    void messageGuardCoversServicesAndAsyncConsumerEntryPoints() {
        AspectJProxyFactory smsFactory = new AspectJProxyFactory(new SmsSendServiceImpl());
        smsFactory.addAspect(new DoormesLocalSafetyConfiguration.LocalOutboundAspect());
        SmsSendService sms = smsFactory.getProxy();
        assertThrows(IllegalStateException.class,
                () -> sms.sendSingleSmsToAdmin(null, 1L, "test", Map.of()));
        assertThrows(IllegalStateException.class, () -> sms.doSendSms(new SmsSendMessage()));

        AspectJProxyFactory mailFactory = new AspectJProxyFactory(new MailSendServiceImpl());
        mailFactory.addAspect(new DoormesLocalSafetyConfiguration.LocalOutboundAspect());
        MailSendService mail = mailFactory.getProxy();
        assertThrows(IllegalStateException.class,
                () -> mail.sendSingleMailToAdmin(1L, null, null, null, "test", Map.of()));
        assertThrows(IllegalStateException.class, () -> mail.doSendMail(new MailSendMessage()));
    }

    @Test
    void unsafeHistoricalConfigIsRejectedBeforeClientInitialization() {
        AtomicInteger initializations = new AtomicInteger();
        FileClientFactory original = new FileClientFactory() {
            @Override
            public FileClient getFileClient(Long configId) {
                throw new AssertionError("A historical client must not be accessed");
            }

            @Override
            public <Config extends FileClientConfig> void createOrUpdateFileClient(
                    Long configId, Integer storage, Config config) {
                initializations.incrementAndGet();
            }
        };
        Path uploadRoot = temporaryDirectory.resolve("uploads");
        DoormesLocalFileSafety safety = new DoormesLocalFileSafety(uploadRoot.toString(),
                "http://127.0.0.1:48082");
        FileClientFactory guarded = safety.guardFactory(original);
        assertThrows(IllegalStateException.class,
                () -> guarded.createOrUpdateFileClient(1L, 20, new S3FileClientConfig()));
        assertThrows(IllegalStateException.class,
                () -> guarded.createOrUpdateFileClient(2L, 10,
                        config(temporaryDirectory.resolve("original-files"), "http://localhost:48082")));
        assertThrows(IllegalStateException.class,
                () -> guarded.createOrUpdateFileClient(3L, 10,
                        config(uploadRoot, "https://invalid.example.test")));
        assertThrows(IllegalStateException.class, () -> guarded.getFileClient(1L));
        assertEquals(0, initializations.get());
    }

    @Test
    void explicitlyConfiguredLanProxyOriginIsAllowedButHistoricalOriginsAreNot() {
        Path uploadRoot = temporaryDirectory.resolve("uploads");
        DoormesLocalFileSafety safety = new DoormesLocalFileSafety(uploadRoot.toString(),
                "http://192.168.50.193:5180");
        assertDoesNotThrow(() -> safety.validateConfig(10,
                config(uploadRoot, "http://192.168.50.193:5180")));
        assertThrows(IllegalStateException.class, () -> safety.validateConfig(10,
                config(uploadRoot, "http://192.168.50.193:48081")));
        assertThrows(IllegalStateException.class, () -> safety.validateConfig(10,
                config(uploadRoot, "http://127.0.0.1:48082")));
    }

    @Test
    void newLocalUploadsCanBeWrittenReadAndDeletedOnlyInsideTheOwnedDirectory() throws Exception {
        try (AnnotationConfigApplicationContext context = context("doormes")) {
            Path uploadRoot = temporaryDirectory.resolve("uploads");
            FileClientFactory factory = context.getBean(FileClientFactory.class);
            LocalFileClientConfig approved = config(uploadRoot, "http://localhost:48082");
            factory.createOrUpdateFileClient(101L, 10, approved);
            // Mutating the caller's config must not alter the initialized safe client.
            approved.setBasePath(temporaryDirectory.resolve("original-files").toString());
            FileClient client = factory.getFileClient(101L);
            byte[] content = "DoorMES test attachment".getBytes(StandardCharsets.UTF_8);
            String url = client.upload(content, "20261002/example.txt", "text/plain");
            assertEquals("http://localhost:48082/admin-api/infra/file/101/get/20261002/example.txt", url);
            assertArrayEquals(content, Files.readAllBytes(uploadRoot.resolve("20261002/example.txt")));
            assertArrayEquals(content, client.getContent("20261002/example.txt"));
            assertFalse(Files.exists(temporaryDirectory.resolve("original-files")));
            assertThrows(IllegalStateException.class,
                    () -> client.upload(content, "../escaped.txt", "text/plain"));
            assertThrows(IllegalStateException.class, () -> client.delete("../../original.txt"));
            assertThrows(IllegalStateException.class, () -> client.getContent("E:\\old\\attachment.txt"));
            assertThrows(IllegalStateException.class, () -> client.getContent("\\\\server\\share\\file.txt"));
            assertThrows(IllegalStateException.class, () -> client.getContent("/original.txt"));
            assertThrows(IllegalStateException.class, () -> client.delete("20261002/example.txt:stream"));
            assertThrows(IllegalStateException.class, () -> client.delete("../.. /original.txt"));
            assertThrows(IllegalStateException.class, () -> client.getContent("NUL.txt"));
            client.delete("20261002/example.txt");
            assertFalse(Files.exists(uploadRoot.resolve("20261002/example.txt")));
        }
    }

    @Test
    void existingSymbolicLinksCannotEscapeTheOwnedDirectory() throws Exception {
        Path uploadRoot = Files.createDirectories(temporaryDirectory.resolve("uploads"));
        Path external = Files.createDirectories(temporaryDirectory.resolve("outside"));
        Path link = uploadRoot.resolve("redirect");
        try {
            Files.createSymbolicLink(link, external);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException unavailable) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "OS does not permit test symlinks");
        }
        DoormesLocalFileSafety safety = new DoormesLocalFileSafety(uploadRoot.toString(),
                "http://127.0.0.1:48082");
        assertThrows(IllegalStateException.class, () -> safety.validateRelativePath("redirect/escaped.txt"));
    }

    private AnnotationConfigApplicationContext context(String profile) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-owned-safety", Map.of(
                "doormes.local.upload-root", temporaryDirectory.resolve("uploads").toString(),
                "server.port", "48082")));
        context.register(TestInfrastructure.class, DoormesLocalSafetyConfiguration.class);
        context.refresh();
        return context;
    }

    private static LocalFileClientConfig config(Path basePath, String domain) {
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setBasePath(basePath.toString());
        config.setDomain(domain);
        return config;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class TestInfrastructure {

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        FileClientFactory fileClientFactory() {
            return new FileClientFactoryImpl();
        }
    }
}
