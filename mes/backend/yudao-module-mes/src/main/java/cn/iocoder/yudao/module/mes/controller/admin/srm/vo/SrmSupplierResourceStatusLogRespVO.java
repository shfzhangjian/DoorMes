package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商资源状态调整日志 Response VO")
@Data
public class SrmSupplierResourceStatusLogRespVO {

    private Long id;
    private String sourceType;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private Long operatorUserId;
    private String operatorUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
