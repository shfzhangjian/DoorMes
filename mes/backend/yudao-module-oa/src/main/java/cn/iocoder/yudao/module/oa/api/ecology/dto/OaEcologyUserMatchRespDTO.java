package cn.iocoder.yudao.module.oa.api.ecology.dto;

import lombok.Data;

/**
 * 泛微 OA 人员匹配 Response DTO。
 */
@Data
public class OaEcologyUserMatchRespDTO {

    private String employeeId;

    private String userId;

    private String username;

    private String tenantKey;

    private String workCode;

    private String mobile;

    private String email;

    private String status;

}
