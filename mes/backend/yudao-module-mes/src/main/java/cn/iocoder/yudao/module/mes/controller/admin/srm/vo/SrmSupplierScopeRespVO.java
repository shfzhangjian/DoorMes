package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 供应商名录管理范围 Response VO")
@Data
public class SrmSupplierScopeRespVO {

    private Long id;
    private String scopeCode;
    private String scopeName;
    private String status;
    private Integer sort;
    private String remark;
    private Integer version;
    private Integer memberCount;
    private List<Member> members;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Data
    public static class Member {

        private Long id;
        private Long scopeId;
        private Long userId;
        private String userName;
        private String permissionLevel;

    }

}
