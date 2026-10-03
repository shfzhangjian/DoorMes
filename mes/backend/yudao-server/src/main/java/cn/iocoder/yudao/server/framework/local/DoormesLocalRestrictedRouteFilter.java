package cn.iocoder.yudao.server.framework.local;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Request isolation for historical plug-ins, not a general network sandbox. */
final class DoormesLocalRestrictedRouteFilter implements Filter {

    // Verified controller mappings from source and the installed Jimu Report/BI JARs.
    private static final List<String> BLOCKED_PREFIXES = List.of(
            "/jmreport", "/jimubi", "/drag",
            "/infra/data-source-config", "/infra/codegen");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (isBlocked(path)) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            httpResponse.getWriter().write("{\"code\":403,\"data\":null,\"msg\":\"doormes 本地安全模式暂不允许历史报表、BI、动态数据源及代码生成入口\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    static boolean isBlocked(String rawPath) {
        String path = rawPath;
        // Resolve encoded separators and dot segments consistently before prefix matching.
        try {
            for (int i = 0; i < 4 && path.contains("%"); i++) {
                path = URLDecoder.decode(path.replace("+", "%2B"), StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException malformed) {
            return true;
        }
        if (path.contains("%") || path.indexOf('\0') >= 0) {
            return true;
        }
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : path.replace('\\', '/').split("/")) {
            segment = segment.split(";", 2)[0];
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (!segments.isEmpty()) {
                    segments.removeLast();
                }
            } else {
                segments.addLast(segment);
            }
        }
        path = "/" + String.join("/", segments);
        if (path.equals("/admin-api")) {
            path = "/";
        } else if (path.startsWith("/admin-api/")) {
            path = path.substring("/admin-api".length());
        }
        for (String prefix : BLOCKED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
