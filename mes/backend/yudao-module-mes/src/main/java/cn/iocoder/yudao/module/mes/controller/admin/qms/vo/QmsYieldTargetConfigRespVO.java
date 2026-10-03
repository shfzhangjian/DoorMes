package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工序合格目标配置 Response VO")
@Data
public class QmsYieldTargetConfigRespVO {

    private Long id;
    private String modelCode;
    private String processCode;
    private String processName;
    private String measureUnit;
    private String targetType;
    private Integer segmentCount;
    private BigDecimal targetQualifiedQty;
    private Integer status;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
