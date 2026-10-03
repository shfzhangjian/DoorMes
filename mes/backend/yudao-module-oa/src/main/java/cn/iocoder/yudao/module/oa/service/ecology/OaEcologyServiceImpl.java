package cn.iocoder.yudao.module.oa.service.ecology;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologyConfigStatusRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologySendMessageRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.ecology.vo.OaEcologyUserRespVO;
import cn.iocoder.yudao.module.oa.framework.config.OaEcologyProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 泛微 Ecology / EMobile OpenAPI MVP 实现。
 */
@Service
@Slf4j
public class OaEcologyServiceImpl implements OaEcologyService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final long TOKEN_EXPIRE_SAFE_WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    @Resource
    private OaEcologyProperties properties;

    private volatile String cachedAccessToken;

    private volatile String cachedAccessTokenCorpId;

    private volatile long cachedAccessTokenExpireAtMillis;

    @Override
    public OaEcologyConfigStatusRespVO getConfigStatus() {
        OaEcologyConfigStatusRespVO respVO = new OaEcologyConfigStatusRespVO();
        respVO.setEnabled(Boolean.TRUE.equals(properties.getEnabled()));
        respVO.setOrigin(properties.getOrigin());
        respVO.setOpenApiOrigin(properties.getOpenApiOrigin());
        respVO.setAppKeyConfigured(StrUtil.isNotBlank(properties.getAppKey()));
        respVO.setAppSecretConfigured(StrUtil.isNotBlank(properties.getAppSecret()));
        respVO.setCorpIdConfigured(StrUtil.isNotBlank(properties.getCorpId()));
        respVO.setAccessTokenConfigured(StrUtil.isNotBlank(properties.getAccessToken()));
        respVO.setSenderTenantKey(properties.getSenderTenantKey());
        respVO.setSenderEmployeeId(properties.getSenderEmployeeId());
        respVO.setSenderName(properties.getSenderName());
        respVO.setEventId(properties.getEventId());
        respVO.setModuleId(properties.getModuleId());
        respVO.setChannels(properties.getChannels());
        return respVO;
    }

    @Override
    public OaEcologySendMessageRespVO sendMessage(OaEcologySendMessageReqVO reqVO) {
        try {
            ensureEnabled();
            OaEcologyUserRespVO receiver = resolveReceiver(reqVO);
            Map<String, Object> body = buildSendMessageBody(reqVO, receiver);
            OaHttpResult httpResult = postJson(buildOpenApiUrl("/api/mc/msg/sendMsg"), body);
            return parseSendMessageResp(httpResult, receiver);
        } catch (Exception ex) {
            log.warn("[sendMessage][泛微 OA 测试消息发送失败：{}]", ex.getMessage());
            OaEcologySendMessageRespVO respVO = new OaEcologySendMessageRespVO();
            respVO.setSuccess(false);
            respVO.setCode("MES_OA_SEND_FAILED");
            respVO.setMessage(ex.getMessage());
            respVO.setRawResponse(ex.getMessage());
            return respVO;
        }
    }

    @Override
    public OaEcologyUserRespVO findEmployeeBySystemUser(String username, String nickname, String mobile) {
        ensureEnabled();
        List<Map<String, Object>> queries = buildEmployeeQueries(username, nickname, mobile);
        for (Map<String, Object> query : queries) {
            OaEcologyUserRespVO user = queryEmployee(query);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    private Map<String, Object> buildSendMessageBody(OaEcologySendMessageReqVO reqVO, OaEcologyUserRespVO receiver) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", obtainAccessToken());
        body.put("title", reqVO.getTitle());
        body.put("text", reqVO.getText());
        putIfNotNull(body, "eventId", defaultIfNull(reqVO.getEventId(), properties.getEventId()));
        putIfNotNull(body, "moduleId", defaultIfNull(reqVO.getModuleId(), properties.getModuleId()));
        body.put("channels", CollUtil.isNotEmpty(reqVO.getChannels()) ? reqVO.getChannels() : properties.getChannels());
        body.put("todo", defaultIfNull(reqVO.getTodo(), properties.getTodo()));
        body.put("outMessage", properties.getOutMessage());
        body.put("sender", buildSender(reqVO));
        body.put("recivers", Collections.singletonList(buildReceiver(reqVO, receiver)));
        body.put("entity", buildEntity(reqVO));
        return body;
    }

    private Map<String, Object> buildSender(OaEcologySendMessageReqVO reqVO) {
        Map<String, Object> sender = new LinkedHashMap<>();
        String tenantKey = StrUtil.blankToDefault(reqVO.getSenderTenantKey(), properties.getSenderTenantKey());
        String employeeId = StrUtil.blankToDefault(reqVO.getSenderEmployeeId(), properties.getSenderEmployeeId());
        String workCode = StrUtil.blankToDefault(reqVO.getSenderWorkCode(), properties.getSenderWorkCode());
        if (StrUtil.isBlank(employeeId) && StrUtil.isBlank(workCode)) {
            throw new IllegalStateException("泛微 OA 默认发送人未配置：请设置 OA_ECOLOGY_SENDER_EMPLOYEE_ID 或 OA_ECOLOGY_SENDER_WORK_CODE");
        }
        putIfNotBlank(sender, "tenantKey", tenantKey);
        putIfNotNull(sender, "employeeId", normalizeIdValue(employeeId));
        putIfNotBlank(sender, "name", StrUtil.blankToDefault(reqVO.getSenderName(), properties.getSenderName()));
        putIfNotBlank(sender, "workCode", workCode);
        return sender;
    }

    private Map<String, Object> buildReceiver(OaEcologySendMessageReqVO reqVO, OaEcologyUserRespVO receiver) {
        Map<String, Object> reciver = new LinkedHashMap<>();
        String tenantKey = StrUtil.blankToDefault(reqVO.getReceiverTenantKey(), receiver.getTenantKey());
        tenantKey = StrUtil.blankToDefault(tenantKey, properties.getSenderTenantKey());
        String employeeId = StrUtil.blankToDefault(reqVO.getReceiverEmployeeId(), receiver.getEmployeeId());
        String workCode = StrUtil.blankToDefault(reqVO.getReceiverWorkCode(), receiver.getWorkCode());
        if (StrUtil.isBlank(employeeId) && StrUtil.isBlank(workCode)) {
            throw new IllegalStateException("泛微 OA 接收人未解析到 employeeId 或 workCode");
        }
        putIfNotBlank(reciver, "tenantKey", tenantKey);
        putIfNotNull(reciver, "employeeId", normalizeIdValue(employeeId));
        putIfNotBlank(reciver, "workCode", workCode);
        putIfNotBlank(reciver, "name", receiver.getUsername());
        return reciver;
    }

    private Map<String, Object> buildEntity(OaEcologySendMessageReqVO reqVO) {
        Map<String, Object> entity = new LinkedHashMap<>();
        putIfNotBlank(entity, "id", StrUtil.blankToDefault(reqVO.getEntityId(),
                "MES-OA-MVP-" + System.currentTimeMillis()));
        putIfNotBlank(entity, "name", StrUtil.blankToDefault(reqVO.getEntityName(), "MES消息MVP"));
        putIfNotBlank(entity, "pcUrl", reqVO.getPcUrl());
        putIfNotBlank(entity, "h5Url", reqVO.getH5Url());
        return entity;
    }

    private OaEcologyUserRespVO resolveReceiver(OaEcologySendMessageReqVO reqVO) {
        OaEcologyUserRespVO receiver = new OaEcologyUserRespVO();
        receiver.setEmployeeId(reqVO.getReceiverEmployeeId());
        receiver.setTenantKey(reqVO.getReceiverTenantKey());
        receiver.setWorkCode(reqVO.getReceiverWorkCode());
        receiver.setUsername(reqVO.getReceiverName());
        if (StrUtil.isNotBlank(receiver.getEmployeeId()) || StrUtil.isNotBlank(receiver.getWorkCode())) {
            return receiver;
        }
        throw new IllegalStateException("泛微 OA 直发模式必须传 receiverEmployeeId 或 receiverWorkCode；"
                + "消息测试页不查询 MES 用户库、不走站内信，也不通过姓名间接解析接收人");
    }

    private OaEcologySendMessageRespVO parseSendMessageResp(OaHttpResult httpResult, OaEcologyUserRespVO receiver) {
        Map<String, Object> response = parseMap(httpResult.getBody());
        String code = firstNotBlank(getString(response, "code"), getString(response, "errcode"),
                getString(response, "errorCode"));
        String message = firstNotBlank(getString(response, "message"), getString(response, "msg"),
                getString(response, "errmsg"), getString(response, "errorMsg"), getString(response, "description"));
        OaEcologySendMessageRespVO respVO = new OaEcologySendMessageRespVO();
        respVO.setHttpStatus(httpResult.getStatus());
        respVO.setCode(code);
        respVO.setMessage(message);
        respVO.setMessageId(extractMessageId(response));
        respVO.setResolvedReceiver(receiver);
        respVO.setRawResponse(httpResult.getBody());
        respVO.setSuccess(isHttpSuccess(httpResult.getStatus()) && isOaSuccess(response, code));
        return respVO;
    }

    private List<Map<String, Object>> buildEmployeeQueries(String username, String nickname, String mobile) {
        List<Map<String, Object>> queries = new ArrayList<>();
        addNameQuery(queries, nickname);
        addAccountQuery(queries, mobile);
        addAccountQuery(queries, username);
        addNameQuery(queries, username);
        return queries;
    }

    private void addAccountQuery(List<Map<String, Object>> queries, String account) {
        if (StrUtil.isBlank(account)) {
            return;
        }
        Map<String, Object> body = buildEmployeeQueryBase();
        body.put("account", account.trim());
        body.put("needAccountInfo", true);
        queries.add(body);
    }

    private void addNameQuery(List<Map<String, Object>> queries, String name) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        Map<String, Object> body = buildEmployeeQueryBase();
        body.put("nameLikeList", Collections.singletonList(name.trim()));
        queries.add(body);
    }

    private Map<String, Object> buildEmployeeQueryBase() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", obtainAccessToken());
        body.put("current", 1);
        body.put("pageSize", 10);
        body.put("userStatusList", Collections.singletonList("normal"));
        body.put("returnFieldList", Arrays.asList("id", "user_id", "username", "tenant_key", "job_num",
                "mobile", "email", "status"));
        return body;
    }

    private OaEcologyUserRespVO queryEmployee(Map<String, Object> body) {
        OaHttpResult httpResult = postJson(buildOpenApiUrl("/api/hrm/restful/queryEmployee"), body);
        Map<String, Object> response = parseMap(httpResult.getBody());
        Map<String, Object> message = getMap(response, "message");
        String errCode = firstNotBlank(getString(message, "errcode"), getString(response, "errcode"),
                getString(response, "code"));
        if (StrUtil.isNotBlank(errCode) && !StrUtil.equalsAnyIgnoreCase(errCode, "0", "200", "SUCCESS", "OK")) {
            log.warn("[queryEmployee][泛微 OA 人员查询失败，code={}, response={}]", errCode, httpResult.getBody());
            return null;
        }
        List<Map<String, Object>> employees = getEmployeeDataList(response);
        if (CollUtil.isEmpty(employees)) {
            return null;
        }
        return toOaUser(employees.get(0));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getEmployeeDataList(Map<String, Object> response) {
        Map<String, Object> data = getMap(response, "data");
        Object list = data.get("data");
        if (list instanceof List<?> rawList) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?, ?> itemMap) {
                    result.add((Map<String, Object>) itemMap);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private OaEcologyUserRespVO toOaUser(Map<String, Object> employee) {
        OaEcologyUserRespVO user = new OaEcologyUserRespVO();
        user.setEmployeeId(firstNotBlank(getString(employee, "id"), getString(employee, "employeeId"),
                getString(employee, "userid")));
        user.setUserId(firstNotBlank(getString(employee, "user_id"), getString(employee, "userId")));
        user.setUsername(firstNotBlank(getString(employee, "username"), getString(employee, "name")));
        user.setTenantKey(firstNotBlank(getString(employee, "tenant_key"), getString(employee, "tenantKey"),
                properties.getSenderTenantKey()));
        user.setWorkCode(firstNotBlank(getString(employee, "job_num"), getString(employee, "jobNum"),
                getString(employee, "workCode")));
        user.setMobile(getString(employee, "mobile"));
        user.setEmail(getString(employee, "email"));
        user.setStatus(getString(employee, "status"));
        return user;
    }

    private synchronized String obtainAccessToken() {
        if (StrUtil.isNotBlank(properties.getAccessToken())) {
            return properties.getAccessToken();
        }
        if (StrUtil.isNotBlank(cachedAccessToken)
                && StrUtil.equals(cachedAccessTokenCorpId, effectiveCorpId())
                && System.currentTimeMillis() < cachedAccessTokenExpireAtMillis - TOKEN_EXPIRE_SAFE_WINDOW_MILLIS) {
            return cachedAccessToken;
        }
        if (StrUtil.isBlank(properties.getAppKey()) || StrUtil.isBlank(properties.getAppSecret())) {
            throw new IllegalStateException("泛微 OA 应用 appKey/appSecret 未配置");
        }

        String code = obtainAuthorizationCode();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_key", properties.getAppKey());
        body.put("app_secret", properties.getAppSecret());
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        OaHttpResult httpResult = postJson(buildOpenApiUrl("/oauth2/access_token"), body);
        Map<String, Object> response = parseMap(httpResult.getBody());
        Map<String, Object> data = getMap(response, "data");
        String accessToken = firstNotBlank(getString(response, "accessToken"), getString(response, "acessToken"),
                getString(response, "access_token"), getString(data, "accessToken"), getString(data, "acessToken"),
                getString(data, "access_token"));
        if (StrUtil.isBlank(accessToken)) {
            throw new IllegalStateException("泛微 OA accessToken 获取失败：" + httpResult.getBody());
        }
        cachedAccessToken = accessToken;
        cachedAccessTokenCorpId = effectiveCorpId();
        Long expiresIn = firstNotNull(getLong(response, "expires_in", null), getLong(data, "expires_in", null),
                getLong(response, "expiresIn", null), getLong(data, "expiresIn", null), 7200L);
        cachedAccessTokenExpireAtMillis = System.currentTimeMillis()
                + Math.max(expiresIn, 60L) * 1000;
        return cachedAccessToken;
    }

    private String obtainAuthorizationCode() {
        if (StrUtil.isNotBlank(properties.getAuthorizationCode())) {
            return properties.getAuthorizationCode();
        }
        String effectiveCorpId = effectiveCorpId();
        if (StrUtil.isBlank(effectiveCorpId)) {
            throw new IllegalStateException("泛微 OA corpId 未配置：请填写开放平台开发者资料中的 corpId，不能使用 tenantKey/all_teams");
        }
        String url = UriComponentsBuilder.fromUriString(buildOpenApiUrl("/oauth2/authorize"))
                .queryParam("corpid", effectiveCorpId)
                .queryParam("response_type", "code")
                .queryParam("state", "MES-OA-MVP")
                .build()
                .toUriString();
        OaHttpResult httpResult = get(url);
        Map<String, Object> response = parseMap(httpResult.getBody());
        Map<String, Object> data = getMap(response, "data");
        String code = firstNotBlank(getString(response, "code"), getString(response, "authorizationCode"),
                getString(data, "code"), getString(data, "authorizationCode"));
        if (StrUtil.isBlank(code) && StrUtil.isNotBlank(httpResult.getBody()) && !httpResult.getBody().contains("{")) {
            code = httpResult.getBody().trim();
        }
        if (StrUtil.isBlank(code)) {
            throw new IllegalStateException("泛微 OA authorization code 获取失败：corpId=" + maskValue(effectiveCorpId)
                    + "，请求参数名=corpid，OA返回=" + httpResult.getBody()
                    + "。请确认该值来自开放平台开发者资料中的 corpId，并且通用型应用已审核通过");
        }
        return code;
    }

    private String effectiveCorpId() {
        return properties.getCorpId();
    }

    private OaHttpResult postJson(String url, Map<String, Object> body) {
        String requestBody = JsonUtils.toJsonString(body);
        log.debug("[postJson][url({}) body({})]", url, maskSecret(requestBody));
        try (HttpResponse response = HttpRequest.post(url)
                .timeout(10_000)
                .contentType(ContentType.JSON.toString())
                .header("Accept", ContentType.JSON.toString())
                .body(requestBody)
                .execute()) {
            return new OaHttpResult(response.getStatus(), response.body());
        }
    }

    private OaHttpResult get(String url) {
        log.debug("[get][url({})]", url);
        try (HttpResponse response = HttpRequest.get(url)
                .timeout(10_000)
                .header("Accept", ContentType.JSON.toString())
                .execute()) {
            return new OaHttpResult(response.getStatus(), response.body());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        String messageId = firstNotBlank(getString(response, "messageId"), getString(response, "msgId"),
                getString(response, "id"));
        Object data = response.get("data");
        if (StrUtil.isNotBlank(messageId) || !(data instanceof Map<?, ?> dataMap)) {
            return messageId;
        }
        return firstNotBlank(getString((Map<String, Object>) dataMap, "messageId"),
                getString((Map<String, Object>) dataMap, "msgId"), getString((Map<String, Object>) dataMap, "id"));
    }

    private Map<String, Object> parseMap(String body) {
        Map<String, Object> response = JsonUtils.parseObjectQuietly(body, MAP_TYPE);
        return response == null ? new HashMap<>() : response;
    }

    private boolean isOaSuccess(Map<String, Object> response, String code) {
        Object success = response.get("success");
        if (success instanceof Boolean successBoolean) {
            return successBoolean;
        }
        Object status = response.get("status");
        if (status instanceof Boolean statusBoolean) {
            return statusBoolean;
        }
        if (StrUtil.isBlank(code)) {
            return !response.containsKey("error") && !response.containsKey("errorCode");
        }
        return StrUtil.equalsAnyIgnoreCase(code, "0", "200", "000000", "SUCCESS", "OK");
    }

    private boolean isHttpSuccess(Integer status) {
        return status != null && status >= 200 && status < 300;
    }

    private String buildOpenApiUrl(String path) {
        String origin = StrUtil.removeSuffix(properties.getOpenApiOrigin(), "/");
        return origin + (StrUtil.startWith(path, "/") ? path : "/" + path);
    }

    private void ensureEnabled() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            throw new IllegalStateException("泛微 OA 对接未启用，请设置 OA_ECOLOGY_ENABLED=true");
        }
    }

    private String getString(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long getLong(Map<String, Object> map, String key, Long defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        if (map == null || !(map.get(key) instanceof Map<?, ?> value)) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) value;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private Object normalizeIdValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        if (!value.matches("\\d+")) {
            return value;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return value;
        }
    }

    private String maskSecret(String requestBody) {
        if (StrUtil.isBlank(requestBody)) {
            return requestBody;
        }
        return requestBody.replaceAll("(\"app_secret\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3")
                .replaceAll("(\"access_token\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
    }

    private String maskValue(String value) {
        if (StrUtil.isBlank(value) || value.length() <= 8) {
            return value;
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    private static final class OaHttpResult {

        private final Integer status;

        private final String body;

        private OaHttpResult(Integer status, String body) {
            this.status = status;
            this.body = body;
        }

        public Integer getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }

    }

}
