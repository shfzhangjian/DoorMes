package cn.iocoder.yudao.server.framework.local;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactory;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.enums.FileStorageEnum;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Never initializes an old FTP/SFTP/S3 client or reads/writes the original local attachment path. */
final class DoormesLocalFileSafety {

    private final Path uploadRoot;
    private final URI fileOrigin;

    DoormesLocalFileSafety(String uploadRoot, String fileOrigin) {
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
        this.fileOrigin = parseOrigin(fileOrigin);
    }

    FileClientFactory guardFactory(FileClientFactory delegate) {
        return new GuardedFileClientFactory(delegate);
    }

    LocalFileClientConfig validateConfig(Integer storage, FileClientConfig config) {
        if (!Integer.valueOf(FileStorageEnum.LOCAL.getStorage()).equals(storage)
                || !(config instanceof LocalFileClientConfig localConfig)) {
            throw denied("旧文件存储类型不可用，只允许新的本地上传存储");
        }
        Path configuredRoot;
        try {
            configuredRoot = Path.of(localConfig.getBasePath()).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw denied("本地上传根目录无效");
        }
        if (!uploadRoot.equals(configuredRoot)) {
            throw denied("旧本地文件目录不可用");
        }
        URI domain = parseOrigin(localConfig.getDomain());
        boolean sameHost = fileOrigin.getHost().equalsIgnoreCase(domain.getHost())
                || (isLoopback(fileOrigin.getHost()) && isLoopback(domain.getHost()));
        if (!fileOrigin.getScheme().equalsIgnoreCase(domain.getScheme())
                || effectivePort(fileOrigin) != effectivePort(domain) || !sameHost) {
            throw denied("文件访问域名不是当前隔离服务");
        }
        rejectLinks(uploadRoot);
        // Defensive copy: a caller cannot mutate an approved configuration into an old path later.
        LocalFileClientConfig safeConfig = new LocalFileClientConfig();
        safeConfig.setBasePath(uploadRoot.toString());
        safeConfig.setDomain(localConfig.getDomain().replaceAll("/+$", ""));
        return safeConfig;
    }

    void validateRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || relativePath.indexOf('\0') >= 0
                || relativePath.indexOf(':') >= 0 || relativePath.startsWith("/")
                || relativePath.startsWith("\\")) {
            throw denied("文件路径必须是上传目录内的相对文件路径");
        }
        String normalizedSeparators = relativePath.replace('\\', '/');
        for (String segment : normalizedSeparators.split("/", -1)) {
            // Windows strips trailing spaces/dots and recognizes DOS device names before I/O.
            String stem = segment.split("\\.", 2)[0];
            if (segment.isEmpty() || segment.equals("..") || segment.equals(".")
                    || segment.endsWith(".") || segment.endsWith(" ")
                    || stem.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) {
                throw denied("文件路径不允许目录跳转或设备名称");
            }
        }
        Path target;
        try {
            target = uploadRoot.resolve(normalizedSeparators).normalize();
        } catch (RuntimeException exception) {
            throw denied("文件路径无效");
        }
        if (target.equals(uploadRoot) || !target.startsWith(uploadRoot)) {
            throw denied("文件路径超出新的本地上传目录");
        }
        // Includes ancestors of the root, existing child links and Windows directory junctions.
        rejectLinks(target);
    }

    private static void rejectLinks(Path target) {
        for (Path current = target; current != null; current = current.getParent()) {
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            try {
                if (Files.isSymbolicLink(current) || !current.equals(current.toRealPath())) {
                    throw denied("本地上传路径不允许符号链接或目录重定向");
                }
            } catch (IOException exception) {
                throw denied("不能安全验证本地上传路径");
            }
        }
    }

    private static URI parseOrigin(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw denied("文件访问配置必须是受控 HTTP 服务 origin");
            }
            return uri;
        } catch (RuntimeException exception) {
            // Never put a historical URL or credentials into the error message.
            throw denied("文件访问配置必须是受控 HTTP 服务 origin");
        }
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "::1".equals(host) || "[::1]".equals(host);
    }

    private static IllegalStateException denied(String message) {
        return new IllegalStateException("doormes 本地安全模式：" + message);
    }

    private final class GuardedFileClientFactory implements FileClientFactory {

        private final FileClientFactory delegate;
        private final Set<Long> approvedConfigIds = ConcurrentHashMap.newKeySet();

        private GuardedFileClientFactory(FileClientFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public <Config extends FileClientConfig> void createOrUpdateFileClient(
                Long configId, Integer storage, Config config) {
            if (configId == null) {
                throw denied("文件配置编号不能为空");
            }
            LocalFileClientConfig safeConfig = validateConfig(storage, config);
            delegate.createOrUpdateFileClient(configId, storage, safeConfig);
            approvedConfigIds.add(configId);
        }

        @Override
        public FileClient getFileClient(Long configId) {
            if (configId == null || !approvedConfigIds.contains(configId)) {
                throw denied("该历史文件配置不可用");
            }
            FileClient client = delegate.getFileClient(configId);
            return client == null ? null : new GuardedFileClient(client);
        }
    }

    private final class GuardedFileClient implements FileClient {

        private final FileClient delegate;

        private GuardedFileClient(FileClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public Long getId() {
            return delegate.getId();
        }

        @Override
        public String upload(byte[] content, String path, String type) throws Exception {
            validateRelativePath(path);
            return delegate.upload(content, path, type);
        }

        @Override
        public void delete(String path) throws Exception {
            validateRelativePath(path);
            delegate.delete(path);
        }

        @Override
        public byte[] getContent(String path) throws Exception {
            validateRelativePath(path);
            return delegate.getContent(path);
        }
    }
}
